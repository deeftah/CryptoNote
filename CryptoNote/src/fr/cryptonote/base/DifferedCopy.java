package fr.cryptonote.base;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) 
public @interface DifferedCopy {
	public Class<?>[] copyToDocs();
	public char separator() default '.';
}
