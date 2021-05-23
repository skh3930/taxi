package texi.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "passengers", path = "passengers")
public interface PassengerRepository extends PagingAndSortingRepository<Passenger, String> {

}
