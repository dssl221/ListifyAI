package com.listifyai.repository;

import com.listifyai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findBySupabaseId(String supabaseId);

    Optional<User> findByEmail(String email);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    @Query("select u.plan, count(u) from User u group by u.plan")
    List<Object[]> countUsersByPlan();

    Page<User> findByStripeSubscriptionIdIsNotNull(Pageable pageable);

    long countByPlan(String plan);

    long countByStripeSubscriptionIdIsNotNull();

    long countByCreatedAtAfter(java.time.OffsetDateTime since);

    @Query(value = """
            select cast(created_at as date) as d, count(*)
              from users
             where created_at >= :since
             group by cast(created_at as date)
             order by d
            """, nativeQuery = true)
    List<Object[]> countSignupsByDaySince(@Param("since") java.time.OffsetDateTime since);
}
