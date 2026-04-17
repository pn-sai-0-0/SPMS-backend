package project.spms.spms.repository;

import project.spms.spms.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    boolean existsByCompanyEmail(String companyEmail);
    boolean existsByCompanyName(String companyName);
    Optional<Company> findByCompanyEmail(String companyEmail);
    Optional<Company> findByCompanyName(String companyName);
}
