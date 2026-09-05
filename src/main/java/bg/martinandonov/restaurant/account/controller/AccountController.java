package bg.martinandonov.restaurant.account.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.account.dto.CurrentAccountResponse;
import bg.martinandonov.restaurant.account.service.AccountService;

@RestController
@RequestMapping("/api/account")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping("/me")
	public ResponseEntity<CurrentAccountResponse> me(Authentication authentication) {
		return ResponseEntity.ok(accountService.getCurrentAccount(authentication));
	}
}
