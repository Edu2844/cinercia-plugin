package net.edu.cinercia.plugin.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface DelegatedByPlatform {
}