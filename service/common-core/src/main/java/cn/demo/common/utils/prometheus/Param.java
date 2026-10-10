package cn.demo.common.utils.prometheus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 指定方法参数在 PromQL 占位符中的名称，例如 #{query.hostName}。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {
  /**
   * 占位符引用的参数名。
   *
   * @return 参数名
   */
  String value();
}
