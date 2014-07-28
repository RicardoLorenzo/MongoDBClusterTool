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

package utils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by ricardolorenzo on 24/07/2014.
 */
public class CappedList<E> extends ArrayList<E> {
    private static final long serialVersionUID = -5043457056558314648L;
    private int size;

    public CappedList(int size) {
        super(size);
        this.size = size;
    }

    @Override
    public boolean add(E o) {
        if(super.size() >= size) {
            super.remove(super.size() - 1);
        }
        super.add(0, o);
        return true;
    }

    @Override
    public void add(int index, E element) {
        if(index >= size - 1) {
            throw new IllegalArgumentException("index is greater than the maximum size");
        }
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if(c.size() > size) {
            throw new IllegalArgumentException("collection is bigger than the maximum size");
        }
        if(super.size() > 0 && super.size() > c.size()) {
            super.removeAll(super.subList(super.size() - c.size(), super.size() - 1));
        } else if(super.size() > 0 && super.size() <= c.size()) {
            super.removeAll(super.subList(0, super.size() - 1));
        }
        return super.addAll(0, c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if(c.size() > size) {
            throw new IllegalArgumentException("collection from index not fit into the maximum size");
        }
        if(super.size() > 0 && super.size() > c.size()) {
            super.removeAll(super.subList(super.size() - c.size(), super.size() - 1));
        } else if(super.size() > 0 && super.size() <= c.size()) {
            super.removeAll(super.subList(0, super.size() - 1));
        }
        return super.addAll(index, c);
    }


}
