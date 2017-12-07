package fr.cryptonote.base;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) 
public @interface ADifferedCopy {
	public Class<?>[] copyToDocs();
	public char separator() default '.';
}
