package com.om.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.om.urlshortener.entity.Click;
import com.om.urlshortener.entity.Url;
import com.om.urlshortener.repository.ClickRepository;

@ExtendWith(MockitoExtension.class)
class ClickServiceTest {

	@Mock
	private ClickRepository clickRepository;

	@Test
	void catchesClickInsertFailuresSoRedirectCanContinue() {
		ClickService service = new ClickService(clickRepository, Runnable::run);
		when(clickRepository.save(any(Click.class))).thenThrow(new RuntimeException("database unavailable"));

		assertThatCode(() -> service.logClick(url(), null, "JUnit", "127.0.0.1"))
				.doesNotThrowAnyException();

		verify(clickRepository).save(any(Click.class));
	}

	@Test
	void catchesExecutorRejectionsSoRedirectCanContinue() {
		ClickService service = new ClickService(clickRepository, command -> {
			throw new RuntimeException("queue full");
		});

		assertThatCode(() -> service.logClick(url(), "https://example.org", "JUnit", "127.0.0.1"))
				.doesNotThrowAnyException();
	}

	private Url url() {
		Url url = Url.pending("https://example.com");
		url.setId(1L);
		url.setShortCode("abc");
		return url;
	}
}
