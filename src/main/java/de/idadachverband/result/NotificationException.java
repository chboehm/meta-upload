package de.idadachverband.result;

/**
 * Created by boehm on 05.11.14.
 */
public class NotificationException extends Exception
{
    private static final long serialVersionUID = 4938256582557279544L;

    public NotificationException(Exception e)
    {
        super(e);
    }
}
