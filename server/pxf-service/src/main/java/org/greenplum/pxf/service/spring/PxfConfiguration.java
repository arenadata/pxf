package org.greenplum.pxf.service.spring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import org.greenplum.pxf.api.configuration.PxfServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures the {@link AsyncTaskExecutor} for tasks that will stream data to
 * clients
 */
@Configuration
@EnableConfigurationProperties(PxfServerProperties.class)
public class PxfConfiguration implements WebMvcConfigurer {

    /**
     * Bean name of PXF's {@link TaskExecutor}.
     */
    public static final String PXF_RESPONSE_STREAM_TASK_EXECUTOR = "pxfResponseStreamTaskExecutor";
    public static final String PXF_EXECUTOR_METRIC_NAME_PREFIX = "pxf";
    public static final String ORIGINAL_EXECUTOR_METRIC_NAME_PREFIX = "executor";
    private static final Logger LOG = LoggerFactory.getLogger(PxfConfiguration.class);

    private final ListableBeanFactory beanFactory;

    /**
     * Constructs a PXF Configuration object with the provided
     * {@link ListableBeanFactory}
     *
     * @param beanFactory the beanFactory
     */
    public PxfConfiguration(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Configures the TaskExecutor to be used for async requests (i.e. Bridge
     * Read).
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        AsyncTaskExecutor taskExecutor = (AsyncTaskExecutor) this.beanFactory
                .getBean(PXF_RESPONSE_STREAM_TASK_EXECUTOR);
        configurer.setTaskExecutor(taskExecutor);
    }

    /**
     * Configures and builds the {@link ThreadPoolTaskExecutor}
     *
     * @return the {@link ThreadPoolTaskExecutor}
     */
    @Bean(name = {PXF_RESPONSE_STREAM_TASK_EXECUTOR,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME})
    public ThreadPoolTaskExecutor pxfApplicationTaskExecutor(PxfServerProperties pxfServerProperties,
                                                             ObjectProvider<ThreadPoolTaskExecutorCustomizer> taskExecutorCustomizers,
                                                             ObjectProvider<TaskDecorator> taskDecorator) {

        TaskExecutionProperties properties = pxfServerProperties.getTask();
        TaskExecutionProperties.Pool pool = properties.getPool();
        ThreadPoolTaskExecutorBuilder builder = new ThreadPoolTaskExecutorBuilder();
        builder = builder.queueCapacity(pool.getQueueCapacity());
        builder = builder.corePoolSize(pool.getCoreSize());
        builder = builder.maxPoolSize(pool.getMaxSize());
        builder = builder.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
        builder = builder.keepAlive(pool.getKeepAlive());
        TaskExecutionProperties.Shutdown shutdown = properties.getShutdown();
        builder = builder.awaitTermination(shutdown.isAwaitTermination());
        builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
        builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
        builder = builder.customizers(taskExecutorCustomizers.orderedStream()::iterator);
        builder = builder.taskDecorator(taskDecorator.getIfUnique());

        LOG.debug("Initializing PXF ThreadPoolTaskExecutor with prefix={}. " +
                        "Pool options: " +
                        "queue capacity={}, core size={}, max size={}, " +
                        "allow core thread timeout={}, keep alive={}. " +
                        "Shutdown options: await termination={}, await " +
                        "termination period={}.",
                properties.getThreadNamePrefix(),
                pool.getQueueCapacity(),
                pool.getCoreSize(),
                pool.getMaxSize(),
                pool.isAllowCoreThreadTimeout(),
                pool.getKeepAlive(),
                shutdown.isAwaitTermination(),
                shutdown.getAwaitTerminationPeriod());

        return builder.build(PxfThreadPoolTaskExecutor.class);
    }

    @Bean
    @ConditionalOnAvailableEndpoint(endpoint = MetricsEndpoint.class)
    public MeterFilter renameMetrics() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (id.getName().startsWith(ORIGINAL_EXECUTOR_METRIC_NAME_PREFIX)) {
                    return id.withName(PXF_EXECUTOR_METRIC_NAME_PREFIX + "." + id.getName());
                }
                return id;
            }
        };
    }

    @Bean
    public MeterBinder registerQueueCapacity(@Qualifier(PXF_RESPONSE_STREAM_TASK_EXECUTOR) ThreadPoolTaskExecutor executor) {
        return (registry) -> Gauge.builder("executor.queue.capacity", executor::getQueueCapacity)
                .tags(Tags.of("name", PXF_RESPONSE_STREAM_TASK_EXECUTOR))
                .description("The max number of threads to be added in queue")
                .baseUnit("tasks")
                .register(registry);
    }
}
