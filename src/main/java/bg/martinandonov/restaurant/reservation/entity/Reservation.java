package bg.martinandonov.restaurant.reservation.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.user.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "reservations",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_reservations_reservation_number",
				columnNames = "reservation_number"),
		indexes = {
				@Index(name = "idx_reservations_table_time", columnList = "dining_table_id, start_time, end_time"),
				@Index(name = "idx_reservations_client_time", columnList = "client_id, start_time"),
				@Index(name = "idx_reservations_status_time", columnList = "status, start_time")
		})
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "reservation_number", nullable = false, length = 36)
	private String reservationNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dining_table_id", nullable = false)
	private DiningTable diningTable;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private AppUser client;

	@Column(name = "start_time", nullable = false)
	private LocalDateTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalDateTime endTime;

	@Column(name = "guest_count", nullable = false)
	private Integer guestCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private ReservationStatus status = ReservationStatus.CONFIRMED;

	@Column(name = "notes", length = 500)
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected Reservation() {
	}

	public Reservation(
			String reservationNumber,
			DiningTable diningTable,
			AppUser client,
			LocalDateTime startTime,
			LocalDateTime endTime,
			Integer guestCount,
			String notes,
			LocalDateTime createdAt) {
		this.reservationNumber = Objects.requireNonNull(reservationNumber, "reservationNumber must not be null");
		this.diningTable = Objects.requireNonNull(diningTable, "diningTable must not be null");
		this.client = Objects.requireNonNull(client, "client must not be null");
		this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
		this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
		this.guestCount = Objects.requireNonNull(guestCount, "guestCount must not be null");
		this.notes = notes;
		this.status = ReservationStatus.CONFIRMED;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getReservationNumber() {
		return reservationNumber;
	}

	public DiningTable getDiningTable() {
		return diningTable;
	}

	public void setDiningTable(DiningTable diningTable) {
		this.diningTable = Objects.requireNonNull(diningTable, "diningTable must not be null");
	}

	public AppUser getClient() {
		return client;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
	}

	public Integer getGuestCount() {
		return guestCount;
	}

	public void setGuestCount(Integer guestCount) {
		this.guestCount = Objects.requireNonNull(guestCount, "guestCount must not be null");
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public void setStatus(ReservationStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public Long getVersion() {
		return version;
	}
}
