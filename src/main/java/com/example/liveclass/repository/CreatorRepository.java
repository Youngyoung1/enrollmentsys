package com.example.liveclass.repository;

import com.example.liveclass.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 강사 Repository
 */
@Repository
public interface CreatorRepository extends JpaRepository<Creator, String> {

}