package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.odin.multimedia.VideoApplication;

@SpringBootTest(classes = VideoApplication.class, properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.datasource.url=jdbc:h2:mem:application-context;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"allowed.file.extensions=jpg,jpeg,png",
		"allowed.file.mime.type=image/jpeg,image/png",
		"file.upload.base.dir=${java.io.tmpdir}/odin-context-test",
		"profile.service.url=http://localhost/",
		"core.data.url=http://localhost/",
		"core.update.url=http://localhost/",
		"video.upload.path=${java.io.tmpdir}/odin-video-context-test"
})
class VideoApplicationTests {

	@Test
	void contextLoads() {
	}

}
