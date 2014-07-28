/*
 * Copyright 2014 Ricardo Lorenzo<unshakablespirit@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package utils.play;

/**
 * Created by ricardolorenzo on 22/07/2014.
 */

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import play.data.Form;
import play.data.validation.Validation;
import play.data.validation.ValidationError;
import play.libs.F;

import javax.validation.ConstraintViolation;
import java.util.*;

/**
 * This class is a patched version of a Play bug.<br>
 * The bug happens when you submit a form with errors and you want to prefill
 * the already entered data.
 *
 * @param <T>
 * @author ndeverge
 */
public class BugWorkaroundForm<T> extends Form<T> {

    private final String rootName;
    private final Class<T> backedType;
    private final Map<String, List<ValidationError>> errors;

    public BugWorkaroundForm(final Class<T> clazz) {

        this(null, clazz);
    }

    @SuppressWarnings("unchecked")
    public BugWorkaroundForm(final String name, final Class<T> clazz) {
        this(name, clazz, new HashMap<String, String>(),
                new HashMap<String, List<ValidationError>>(), play.libs.F.None());
    }

    /**
     * Creates a new <code>Form</code>.
     *
     * @param clazz  wrapped class
     * @param data   the current form data (used to display the form)
     * @param errors the collection of errors associated with this form
     * @param value  optional concrete value of type <code>T</code> if the form
     *               submission was successful
     */
    public BugWorkaroundForm(final String rootName, final Class<T> clazz,
                             final Map<String, String> data,
                             final Map<String, List<ValidationError>> errors,
                             final F.Option<T> value) {
        super(rootName, clazz, data, errors, value);
        this.rootName = rootName;
        this.backedType = clazz;
        this.errors = errors;
    }

    private T blankInstance() {
        try {
            return backedType.newInstance();
        } catch(Exception e) {
            throw new RuntimeException("Cannot instantiate " + backedType
                    + ". It must have a default constructor", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Form<T> bind(final Map<String, String> data, final String... allowedFields) {
        DataBinder dataBinder = null;
        Map<String, String> objectData = data;
        if(rootName == null) {
            dataBinder = new DataBinder(blankInstance());
        } else {
            dataBinder = new DataBinder(blankInstance(), rootName);
            objectData = new HashMap<String, String>();
            for(String key : data.keySet()) {
                if(key.startsWith(rootName + ".")) {
                    objectData.put(key.substring(rootName.length() + 1), data.get(key));
                }
            }
        }
        if(allowedFields.length > 0) {
            dataBinder.setAllowedFields(allowedFields);
        }
        SpringValidatorAdapter validator = new SpringValidatorAdapter(Validation.getValidator());
        dataBinder.setValidator(validator);
        dataBinder.setConversionService(play.data.format.Formatters.conversion);
        dataBinder.setAutoGrowNestedPaths(true);
        dataBinder.bind(new MutablePropertyValues(objectData));

        Set<ConstraintViolation<Object>> validationErrors = validator.validate(dataBinder.getTarget());
        BindingResult result = dataBinder.getBindingResult();

        for(ConstraintViolation<Object> violation : validationErrors) {
            String field = violation.getPropertyPath().toString();
            FieldError fieldError = result.getFieldError(field);
            if(fieldError == null || !fieldError.isBindingFailure()) {
                try {
                    result.rejectValue(field,
                            violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                            getArgumentsForConstraint(result.getObjectName(), field, violation.getConstraintDescriptor()),
                            violation.getMessage());
                } catch(NotReadablePropertyException ex) {
                    throw new IllegalStateException("JSR-303 validated property '" + field +
                            "' does not have a corresponding accessor for data binding - " +
                            "check your DataBinder's configuration (bean property versus direct field access)", ex);
                }
            }
        }

        if(result.hasErrors()) {
            Map<String, List<ValidationError>> errors = new HashMap<String, List<ValidationError>>();
            for(FieldError error : result.getFieldErrors()) {
                String key = error.getObjectName() + "." + error.getField();
                System.out.println("Error field:" + key);
                if(key.startsWith("target.") && rootName == null) {
                    key = key.substring(7);
                }
                List<Object> arguments = new ArrayList<>();
                for(Object arg : error.getArguments()) {
                    if(!(arg instanceof org.springframework.context.support.DefaultMessageSourceResolvable)) {
                        arguments.add(arg);
                    }
                }
                if(!errors.containsKey(key)) {
                    errors.put(key, new ArrayList<ValidationError>());
                }
                errors.get(key).add(new ValidationError(key, error.isBindingFailure() ? "error.invalid"
                        : error.getDefaultMessage(), arguments));
            }
            return new Form(rootName, backedType, data, errors, F.Option.None());
        } else {
            Object globalError = null;
            if(result.getTarget() != null) {
                try {
                    java.lang.reflect.Method v = result.getTarget().getClass().getMethod("validate");
                    globalError = v.invoke(result.getTarget());
                } catch(NoSuchMethodException e) {
                } catch(Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            if(globalError != null) {
                Map<String, List<ValidationError>> errors = new HashMap<String, List<ValidationError>>();
                if(globalError instanceof String) {
                    errors.put("", new ArrayList<ValidationError>());
                    errors.get("").add(
                            new ValidationError("", (String) globalError, new ArrayList()));
                } else if(globalError instanceof List) {
                    for(ValidationError error : (List<ValidationError>) globalError) {
                        List<ValidationError> errorsForKey = errors.get(error.key());
                        if(errorsForKey == null) {
                            errors.put(error.key(), errorsForKey = new ArrayList<ValidationError>());
                        }
                        errorsForKey.add(error);
                    }
                } else if(globalError instanceof Map) {
                    errors = (Map<String, List<ValidationError>>) globalError;
                }

                if(result.getTarget() != null) {
                    return new Form(rootName, backedType, data, errors, F.Option.Some((T) result.getTarget()));
                } else {
                    return new Form(rootName, backedType, data, errors, F.Option.None());
                }
            }
            return new Form(rootName, backedType, new HashMap<String, String>(data), new HashMap<String,
                    List<ValidationError>>(errors), F.Option.Some((T) result.getTarget()));
        }
    }
}