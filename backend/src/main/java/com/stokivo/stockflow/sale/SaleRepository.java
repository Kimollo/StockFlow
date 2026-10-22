package com.stokivo.stockflow.sale;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

	@Query("""
			select distinct sale
			from Sale sale
			left join fetch sale.items
			where sale.soldAt >= :start and sale.soldAt < :end
			""")
	List<Sale> findWithItemsBetween(@Param("start") Instant start, @Param("end") Instant end);

	Page<Sale> findByCashier_Id(Long cashierId, Pageable pageable);
}
