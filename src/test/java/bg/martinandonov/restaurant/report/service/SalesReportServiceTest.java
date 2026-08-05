package bg.martinandonov.restaurant.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.payment.entity.PaymentMethod;
import bg.martinandonov.restaurant.report.dto.MenuItemSalesReportResponse;
import bg.martinandonov.restaurant.report.dto.PaymentMethodSalesReportResponse;
import bg.martinandonov.restaurant.report.dto.SalesSummaryResponse;
import bg.martinandonov.restaurant.report.projection.MenuItemSalesProjection;
import bg.martinandonov.restaurant.report.projection.PaymentMethodSalesProjection;
import bg.martinandonov.restaurant.report.projection.PaymentSummaryProjection;
import bg.martinandonov.restaurant.report.projection.SoldItemsSummaryProjection;
import bg.martinandonov.restaurant.report.repository.SalesReportRepository;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Europe/Sofia");
	private static final LocalDateTime FROM = LocalDateTime.of(2026, 8, 1, 0, 0);
	private static final LocalDateTime TO = LocalDateTime.of(2026, 8, 2, 0, 0);

	@Mock
	private SalesReportRepository salesReportRepository;

	private SalesReportService salesReportService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZONE);
		salesReportService = new SalesReportService(salesReportRepository, clock);
	}

	@Test
	void summaryComputesMetricsWithHalfUpAverage() {
		when(salesReportRepository.summarizePayments(FROM, TO)).thenReturn(summary(2L, "100.00"));
		when(salesReportRepository.summarizeSoldItems(FROM, TO)).thenReturn(sold(5L));

		SalesSummaryResponse response = salesReportService.getSalesSummary(FROM, TO);

		assertThat(response.getPeriod().getFrom()).isEqualTo(FROM);
		assertThat(response.getPeriod().getTo()).isEqualTo(TO);
		assertThat(response.getPeriod().getTimeZone()).isEqualTo("Europe/Sofia");
		assertThat(response.getTotalRevenue()).isEqualByComparingTo("100.00");
		assertThat(response.getPaidOrdersCount()).isEqualTo(2L);
		assertThat(response.getSoldItemsCount()).isEqualTo(5L);
		assertThat(response.getAverageOrderValue()).isEqualByComparingTo("50.00");
	}

	@Test
	void summaryAverageUsesHalfUpRounding() {
		when(salesReportRepository.summarizePayments(FROM, TO)).thenReturn(summary(3L, "10.00"));
		when(salesReportRepository.summarizeSoldItems(FROM, TO)).thenReturn(sold(3L));

		SalesSummaryResponse response = salesReportService.getSalesSummary(FROM, TO);

		assertThat(response.getAverageOrderValue()).isEqualByComparingTo("3.33");
	}

	@Test
	void summaryNormalizesNullAggregatesToZero() {
		when(salesReportRepository.summarizePayments(FROM, TO)).thenReturn(summary(null, null));
		when(salesReportRepository.summarizeSoldItems(FROM, TO)).thenReturn(sold(null));

		SalesSummaryResponse response = salesReportService.getSalesSummary(FROM, TO);

		assertThat(response.getTotalRevenue()).isEqualByComparingTo("0.00");
		assertThat(response.getPaidOrdersCount()).isZero();
		assertThat(response.getSoldItemsCount()).isZero();
		assertThat(response.getAverageOrderValue()).isEqualByComparingTo("0.00");
	}

	@Test
	void summaryRejectsInvalidPeriod() {
		assertThatThrownBy(() -> salesReportService.getSalesSummary(null, TO))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("from");
		assertThatThrownBy(() -> salesReportService.getSalesSummary(FROM, null))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("to");
		assertThatThrownBy(() -> salesReportService.getSalesSummary(FROM, FROM))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("before");
		assertThatThrownBy(() -> salesReportService.getSalesSummary(TO, FROM))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("before");
	}

	@Test
	void byItemMapsProjectionRows() {
		when(salesReportRepository.aggregateSalesByMenuItem(FROM, TO)).thenReturn(List.of(
				item(1L, "Soup Snapshot", 4L, "40.00", 2L),
				item(2L, "Salad", 1L, "12.50", 1L)));

		MenuItemSalesReportResponse response = salesReportService.getSalesByMenuItem(FROM, TO);

		assertThat(response.getItems()).hasSize(2);
		assertThat(response.getItems().get(0).getMenuItemName()).isEqualTo("Soup Snapshot");
		assertThat(response.getItems().get(0).getRevenue()).isEqualByComparingTo("40.00");
		assertThat(response.getItems().get(0).getQuantitySold()).isEqualTo(4L);
		assertThat(response.getItems().get(0).getPaidOrdersCount()).isEqualTo(2L);
	}

	@Test
	void byItemEmptyWhenNoData() {
		when(salesReportRepository.aggregateSalesByMenuItem(FROM, TO)).thenReturn(List.of());
		assertThat(salesReportService.getSalesByMenuItem(FROM, TO).getItems()).isEmpty();
	}

	@Test
	void byPaymentMethodAlwaysIncludesCashAndCard() {
		when(salesReportRepository.aggregateSalesByPaymentMethod(FROM, TO)).thenReturn(List.of(
				method(PaymentMethod.CASH, 1L, "30.00")));

		PaymentMethodSalesReportResponse response = salesReportService.getSalesByPaymentMethod(FROM, TO);

		assertThat(response.getTotalRevenue()).isEqualByComparingTo("30.00");
		assertThat(response.getMethods()).hasSize(2);
		assertThat(response.getMethods().get(0).getMethod()).isEqualTo(PaymentMethod.CASH);
		assertThat(response.getMethods().get(0).getPercentageOfRevenue()).isEqualByComparingTo("100.00");
		assertThat(response.getMethods().get(1).getMethod()).isEqualTo(PaymentMethod.CARD);
		assertThat(response.getMethods().get(1).getPaymentCount()).isZero();
		assertThat(response.getMethods().get(1).getAmount()).isEqualByComparingTo("0.00");
		assertThat(response.getMethods().get(1).getPercentageOfRevenue()).isEqualByComparingTo("0.00");
	}

	@Test
	void byPaymentMethodComputesPercentages() {
		when(salesReportRepository.aggregateSalesByPaymentMethod(FROM, TO)).thenReturn(List.of(
				method(PaymentMethod.CASH, 1L, "25.00"),
				method(PaymentMethod.CARD, 1L, "75.00")));

		PaymentMethodSalesReportResponse response = salesReportService.getSalesByPaymentMethod(FROM, TO);

		assertThat(response.getTotalRevenue()).isEqualByComparingTo("100.00");
		assertThat(response.getMethods().get(0).getPercentageOfRevenue()).isEqualByComparingTo("25.00");
		assertThat(response.getMethods().get(1).getPercentageOfRevenue()).isEqualByComparingTo("75.00");
	}

	@Test
	void byPaymentMethodZeroPeriodReturnsZeroPercentages() {
		when(salesReportRepository.aggregateSalesByPaymentMethod(FROM, TO)).thenReturn(List.of());

		PaymentMethodSalesReportResponse response = salesReportService.getSalesByPaymentMethod(FROM, TO);

		assertThat(response.getTotalRevenue()).isEqualByComparingTo("0.00");
		assertThat(response.getMethods()).extracting(r -> r.getMethod())
				.containsExactly(PaymentMethod.CASH, PaymentMethod.CARD);
		assertThat(response.getMethods()).allMatch(r -> r.getPercentageOfRevenue().compareTo(BigDecimal.ZERO) == 0);
	}

	private PaymentSummaryProjection summary(Long count, String amount) {
		return new PaymentSummaryProjection() {
			@Override
			public Long getPaymentCount() {
				return count;
			}

			@Override
			public BigDecimal getTotalAmount() {
				return amount == null ? null : new BigDecimal(amount);
			}
		};
	}

	private SoldItemsSummaryProjection sold(Long count) {
		return () -> count;
	}

	private MenuItemSalesProjection item(Long id, String name, Long qty, String revenue, Long orders) {
		return new MenuItemSalesProjection() {
			@Override
			public Long getMenuItemId() {
				return id;
			}

			@Override
			public String getMenuItemName() {
				return name;
			}

			@Override
			public Long getQuantitySold() {
				return qty;
			}

			@Override
			public BigDecimal getRevenue() {
				return new BigDecimal(revenue);
			}

			@Override
			public Long getPaidOrdersCount() {
				return orders;
			}
		};
	}

	private PaymentMethodSalesProjection method(PaymentMethod method, Long count, String amount) {
		return new PaymentMethodSalesProjection() {
			@Override
			public PaymentMethod getMethod() {
				return method;
			}

			@Override
			public Long getPaymentCount() {
				return count;
			}

			@Override
			public BigDecimal getAmount() {
				return new BigDecimal(amount);
			}
		};
	}
}