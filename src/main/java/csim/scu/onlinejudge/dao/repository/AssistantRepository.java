package csim.scu.onlinejudge.dao.repository;

import csim.scu.onlinejudge.dao.domain.assistant.Assistant;
import csim.scu.onlinejudge.dao.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssistantRepository extends BaseRepository<Assistant, Long> {

    boolean existsByAccount(String account);

    Optional<Assistant> findByAccount(String account);

    Optional<Assistant> findByName(String name);

    @Modifying(clearAutomatically = true)
    @Query("update Assistant set password=:newPassword where account=:account AND password=:oriPassword")
    int updatePasswordByAccountAndPassword(@Param("account")String account,
                                           @Param("oriPassword")String oriPassword,
                                           @Param("newPassword")String newPassword);

}
