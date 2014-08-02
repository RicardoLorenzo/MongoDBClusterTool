package utils.test;

/**
 * Created by ricardolorenzo on 01/08/2014.
 */
public class TestException extends Exception {
    public TestException() {
    }

    public TestException(String message) {
        super(message);
    }

    public TestException(Throwable e) {
        super(e.getMessage());
    }
}
