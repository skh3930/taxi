package texi.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "texis", path = "texis")
public interface TexiRepository extends PagingAndSortingRepository<Texi, Long> {

}
