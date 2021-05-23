package texi.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "calls", path = "calls")
public interface CallRepository extends PagingAndSortingRepository<Call, Long> {

}
