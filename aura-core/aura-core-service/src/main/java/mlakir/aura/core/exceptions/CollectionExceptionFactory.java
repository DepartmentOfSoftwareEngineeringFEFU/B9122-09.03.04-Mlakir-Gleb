package mlakir.aura.core.exceptions;

import mlakir.aura.core.exceptions.AuraExceptionFactorySupport;
import mlakir.aura.exception.AuraException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CollectionExceptionFactory extends AuraExceptionFactorySupport {

    public AuraException collectionJobNotFound(Long jobId) {
        return build(HttpStatus.NOT_FOUND, "Collection job not found",
                "Collection job with id=" + jobId + " was not found.", "collection_job_not_found");
    }

    public AuraException collectionJobAlreadyRunning(Long sourceId) {
        return build(HttpStatus.CONFLICT, "Collection job already running",
                "Collection job for source id=" + sourceId + " is already running.",
                "collection_job_already_running");
    }
}
