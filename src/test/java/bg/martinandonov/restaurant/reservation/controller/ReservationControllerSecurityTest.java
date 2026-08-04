package bg.martinandonov.restaurant.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import bg.martinandonov.restaurant.common.exception.GlobalExceptionHandler;
import bg.martinandonov.restaurant.reservation.dto.ReservationAvailabilityResponse;
import bg.martinandonov.restaurant.reservation.dto.ReservationResponse;
import bg.martinandonov.restaurant.reservation.dto.ReservationScheduleEntryResponse;
import bg.martinandonov.restaurant.reservation.service.ReservationService;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = {
		ClientReservationController.class,
		AdminReservationController.class,
		WaiterReservationController.class
})
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ReservationControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservationService reservationService;

	@Test
	void anonymousDenied() throws Exception {
		mockMvc.perform(get("/api/client/reservations")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/admin/reservations")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/waiter/reservations/schedule")
						.param("from", "2026-08-05T10:00:00")
						.param("to", "2026-08-05T22:00:00"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void clientCanAccessClientEndpoints() throws Exception {
		when(reservationService.getCurrentClientReservations()).thenReturn(List.of());
		when(reservationService.findAvailableTables(any(), any(), any()))
				.thenReturn(new ReservationAvailabilityResponse(
						LocalDateTime.parse("2026-08-05T19:00:00"),
						LocalDateTime.parse("2026-08-05T21:00:00"),
						2,
						List.of()));
		when(reservationService.createForCurrentClient(any())).thenReturn(sampleResponse());

		mockMvc.perform(get("/api/client/reservations").with(user("c").roles("CLIENT")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/client/reservations/availability")
						.param("startTime", "2026-08-05T19:00:00")
						.param("endTime", "2026-08-05T21:00:00")
						.param("guestCount", "2")
						.with(user("c").roles("CLIENT")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/client/reservations")
						.with(user("c").roles("CLIENT"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"diningTableId":1,"startTime":"2026-08-05T19:00:00","endTime":"2026-08-05T21:00:00","guestCount":2}
								"""))
				.andExpect(status().isCreated());
	}

	@Test
	void clientDeniedAdminAndWaiter() throws Exception {
		mockMvc.perform(get("/api/admin/reservations").with(user("c").roles("CLIENT")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/waiter/reservations/schedule")
						.param("from", "2026-08-05T10:00:00")
						.param("to", "2026-08-05T22:00:00")
						.with(user("c").roles("CLIENT")))
				.andExpect(status().isForbidden());
	}

	@Test
	void waiterCanReadScheduleOnly() throws Exception {
		when(reservationService.getSchedule(any(), any(), isNull(), isNull(), eq(true)))
				.thenReturn(List.of());

		mockMvc.perform(get("/api/waiter/reservations/schedule")
						.param("from", "2026-08-05T10:00:00")
						.param("to", "2026-08-05T22:00:00")
						.with(user("w").roles("WAITER")))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/client/reservations")
						.with(user("w").roles("WAITER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/reservations")
						.with(user("w").roles("WAITER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void cookDenied() throws Exception {
		mockMvc.perform(get("/api/client/reservations").with(user("k").roles("COOK")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/reservations").with(user("k").roles("COOK")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/waiter/reservations/schedule")
						.param("from", "2026-08-05T10:00:00")
						.param("to", "2026-08-05T22:00:00")
						.with(user("k").roles("COOK")))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanAccessAdminAndWaiter() throws Exception {
		when(reservationService.createByAdmin(any())).thenReturn(sampleResponse());
		when(reservationService.getSchedule(any(), any(), isNull(), isNull(), anyBoolean()))
				.thenReturn(List.of(sampleSchedule()));

		mockMvc.perform(post("/api/admin/reservations")
						.with(user("a").roles("ADMIN"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientId":10,"diningTableId":1,"startTime":"2026-08-05T19:00:00","endTime":"2026-08-05T21:00:00","guestCount":2}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/admin/reservations/schedule")
						.param("from", "2026-08-05T10:00:00")
						.param("to", "2026-08-05T22:00:00")
						.with(user("a").roles("ADMIN")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/waiter/reservations/schedule")
						.param("from", "2026-08-05T10:00:00")
						.param("to", "2026-08-05T22:00:00")
						.with(user("a").roles("ADMIN")))
				.andExpect(status().isOk());
	}

	private static ReservationResponse sampleResponse() {
		return new ReservationResponse(
				1L, "num", 1L, 5, "Window", 10L, "Client", "client@example.com",
				LocalDateTime.parse("2026-08-05T19:00:00"),
				LocalDateTime.parse("2026-08-05T21:00:00"),
				2, "CONFIRMED", null,
				LocalDateTime.parse("2026-08-05T12:00:00"),
				LocalDateTime.parse("2026-08-05T12:00:00"));
	}

	private static ReservationScheduleEntryResponse sampleSchedule() {
		return new ReservationScheduleEntryResponse(
				1L, "num", 1L, 5, 10L, "Client",
				LocalDateTime.parse("2026-08-05T19:00:00"),
				LocalDateTime.parse("2026-08-05T21:00:00"),
				2, "CONFIRMED");
	}
}
