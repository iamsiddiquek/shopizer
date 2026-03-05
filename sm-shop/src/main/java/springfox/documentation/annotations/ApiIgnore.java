package springfox.documentation.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MIGRATION NOTE: Springfox was removed for Spring Boot 3 compatibility. This compatibility annotation preserves existing source signatures until controller documentation annotations are fully migrated.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface ApiIgnore {
}
