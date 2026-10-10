package cn.demo.common.utils.prometheus;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;
import java.util.Map;

/**
 * 由 {@link EnablePrometheusMappers} 导入，扫描标记接口并将其代理注册为 Spring 单例 Bean。
 *
 * <p>注册阶段只保存代理创建器，不会实例化真实查询客户端；方法调用时由
 * {@link PromMapperFactory} 输出渲染后的 PromQL。</p>
 */
public class PrometheusMapperRegistrar
        implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
                EnablePrometheusMappers.class.getName());
        if (attributes == null) {
            return;
        }

        // Spring 默认扫描器只接受具体类，这里允许独立接口成为候选对象。
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                        return definition.getMetadata().isIndependent();
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(PrometheusMapper.class));

        PromMapperFactory factory = beanFactory.getBean(PromMapperFactory.class);
        String[] basePackages = (String[]) attributes.get("basePackages");
        if (basePackages == null || basePackages.length == 0) {
            throw new IllegalArgumentException("At least one Prometheus mapper base package is required");
        }
        for (String basePackage : basePackages) {
            if (basePackage == null || basePackage.isBlank()) {
                throw new IllegalArgumentException("Prometheus mapper base package must not be blank");
            }
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                String className = candidate.getBeanClassName();
                if (className == null) {
                    continue;
                }
                Class<?> mapperType = ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());
                if (!mapperType.isInterface()) {
                    throw new IllegalStateException(
                            "@PrometheusMapper must annotate an interface: " + className);
                }

                // 使用常规 Bean 名，兼容示例中的 @Resource 以及按接口类型注入。
                String beanName = Introspector.decapitalize(mapperType.getSimpleName());
                /*如果bean已经注册到容器中*/
                if (registry.containsBeanDefinition(beanName)) {
                    BeanDefinition existing = registry.getBeanDefinition(beanName);
                    if (mapperType.getName().equals(existing.getBeanClassName())) {
                        // 重叠扫描包或重复启用时，同一 Mapper 只注册一次。
                        continue;
                    }
                    throw new IllegalStateException("Conflicting Prometheus mapper bean name: " + beanName);
                }
                // Bean 类型仍声明为接口，便于 Spring 按接口类型注入；实例由 Supplier 返回 JDK 代理。
                RootBeanDefinition definition = new RootBeanDefinition(mapperType);
                definition.setTargetType(mapperType);
                definition.setInstanceSupplier(() -> factory.createMapper(mapperType));
                registry.registerBeanDefinition(beanName, definition);
            }
        }
    }

}
