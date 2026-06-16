package nl.vpro.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DRepository extends JpaRepository<D, Long> {
}
