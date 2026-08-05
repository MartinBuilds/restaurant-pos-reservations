package bg.martinandonov.restaurant.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.payment.entity.PaymentMethod;
import bg.martinandonov.restaurant.report.dto.MenuItemSalesReportResponse;
import bg.martinandonov.restaurant.report.dto.MenuItemSalesRowResponse;
import bg.martinandonov.restaurant.report.dto.PaymentMethodSalesReportResponse;
import bg.martinandonov.restaurant.report.dto.PaymentMethodSalesRowResponse;
import bg.martinandonov.restaurant.report.dto.ReportPeriodResponse;
import bg.martinandonov.restaurant.report.dto.SalesSummaryResponse;
import bg.martinandonov.restaurant.report.projection.MenuItemSalesProjection;
import bg.martinandonov.restaurant.report.projection.PaymentMethodSalesProjection;
import bg.martinandonov.restaurant.report.projection.PaymentSummaryProjection;
import bg.martinandonov.restaurant.report.projection.SoldItemsSummaryProjection;
import bg.martinandonov.restaurant.report.repository.SalesReportRepository;

@Service
@Transactional(readOnly = true)
public class SalesReportService {

	private static final int MONEY_SCALE = 2;
	private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
	private static final BigDecimal HUNDRED = new BigDecimal("100");

	private final SalesReportRepository salesReportRepository;
	private final Clock clock;

	public SalesReportService(SalesReportRepository salesReportRepository, Clock clock) {
		this.salesReportRepository = salesReportRepository;
		this.clock = clock;
	}

	public SalesSummaryResponse getSalesSummary(LocalDateTime from, LocalDateTime to) {
		ReportPeriodResponse period = requirePeriod(from, to);
		PaymentSummaryProjection paymentSummary = salesReportRepository.summarizePayments(from, to);
		SoldItemsSummaryProjection soldItemsSummary = salesReportRepository.summarizeSoldItems(from, to);

		BigDecimal totalRevenue = money(paymentSummary == null ? null : paymentSummary.getTotalAmount());
		long paidOrdersCount = longValue(paymentSummary == null ? null : paymentSummary.getPaymentCount());
		long soldItemsCount = longValue(soldItemsSummary == null ? null : soldItemsSummary.getSoldItemsCount());
		BigDecimal averageOrderValue = average(totalRevenue, paidOrdersCount);

		return new SalesSummaryResponse(period, totalRevenue, paidOrdersCount, soldItemsCount, averageOrderValue);
	}

	public MenuItemSalesReportResponse getSalesByMenuItem(LocalDateTime from, LocalDateTime to) {
		ReportPeriodResponse period = requirePeriod(from, to);
		List<MenuItemSalesProjection> rows = salesReportRepository.aggregateSalesByMenuItem(from, to);
		List<MenuItemSalesRowResponse> items = rows.stream()
				.map(row -> new MenuItemSalesRowResponse(
						row.getMenuItemId(),
						row.getMenuItemName(),
						longValue(row.getQuantitySold()),
						money(row.getRevenue()),
						longValue(row.getPaidOrdersCount())))
				.toList();
		return new MenuItemSalesReportResponse(period, items);
	}

	public PaymentMethodSalesReportResponse getSalesByPaymentMethod(LocalDateTime from, LocalDateTime to) {
		ReportPeriodResponse period = requirePeriod(from, to);
		List<PaymentMethodSalesProjection> rows = salesReportRepository.aggregateSalesByPaymentMethod(from, to);

		Map<PaymentMethod, PaymentMethodSalesProjection> byMethod = new EnumMap<>(PaymentMethod.class);
		for (PaymentMethodSalesProjection row : rows) {
			if (row.getMethod() != null) {
				byMethod.put(row.getMethod(), row);
			}
		}

		BigDecimal totalRevenue = money(BigDecimal.ZERO);
		for (PaymentMethod method : List.of(PaymentMethod.CASH, PaymentMethod.CARD)) {
			PaymentMethodSalesProjection row = byMethod.get(method);
			totalRevenue = totalRevenue.add(money(row == null ? null : row.getAmount()));
		}

		List<PaymentMethodSalesRowResponse> methods = new ArrayList<>(2);
		for (PaymentMethod method : List.of(PaymentMethod.CASH, PaymentMethod.CARD)) {
			PaymentMethodSalesProjection row = byMethod.get(method);
			long paymentCount = longValue(row == null ? null : row.getPaymentCount());
			BigDecimal amount = money(row == null ? null : row.getAmount());
			methods.add(new PaymentMethodSalesRowResponse(
					method,
					paymentCount,
					amount,
					percentage(amount, totalRevenue)));
		}

		return new PaymentMethodSalesReportResponse(period, totalRevenue, methods);
	}

	private ReportPeriodResponse requirePeriod(LocalDateTime from, LocalDateTime to) {
		if (from == null) {
			throw new InvalidRequestException("from must be provided");
		}
		if (to == null) {
			throw new InvalidRequestException("to must be provided");
		}
		if (!from.isBefore(to)) {
			throw new InvalidRequestException("from must be before to");
		}
		return new ReportPeriodResponse(from, to, clock.getZone().getId());
	}

	private BigDecimal money(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
		}
		return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
	}

	private long longValue(Long value) {
		return value == null ? 0L : value;
	}

	private BigDecimal average(BigDecimal totalRevenue, long paidOrdersCount) {
		if (paidOrdersCount <= 0L) {
			return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
		}
		return totalRevenue.divide(BigDecimal.valueOf(paidOrdersCount), MONEY_SCALE, MONEY_ROUNDING);
	}

	private BigDecimal percentage(BigDecimal amount, BigDecimal totalRevenue) {
		if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
		}
		return amount.multiply(HUNDRED).divide(totalRevenue, MONEY_SCALE, MONEY_ROUNDING);
	}
}