package com.odin.multimedia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.odin.multimedia.entity.ResponseMessages;


@Repository
public interface ResponseMessagesRepository extends JpaRepository<ResponseMessages, Integer> {

}
