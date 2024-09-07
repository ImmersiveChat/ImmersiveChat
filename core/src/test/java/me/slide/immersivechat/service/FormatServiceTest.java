package me.slide.immersivechat.service;

import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.model.Filter;
import me.slide.immersivechat.xcut.VersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FormatServiceTest {
	@Mock
	private ImmersiveChat plugin;
	@Mock
	private PlayerApiService playerApiService;
	@Mock
	private ConfigService configService;
	@Mock
	private VersionService versionService;
	@InjectMocks
	private FormatService formatService;

	private static final List<Filter> FILTERS = List.of(new Filter("(b[i1]a?tch(es)?)", "puppy"));

	@Test
	public void testFilter() {
		when(configService.getFilters()).thenReturn(FILTERS);
		final String input = "You are a bitch!";
		final String expected = "You are a puppy!";
		final String actual = formatService.filterChat(input);
		assertEquals(expected, actual);
	}
}
