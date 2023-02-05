/*
 * SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
 *
 * SPDX-License-Identifier: CC-BY-SA-4.0
 *
 * Modified and adapted from https://stackoverflow.com/a/39194410/15401262
 *
 */

package com.exifthumbnailadder.app;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention( RetentionPolicy.RUNTIME )
@Target({ METHOD, ANNOTATION_TYPE })
public @interface Repeat {
    int value() default 1;
}