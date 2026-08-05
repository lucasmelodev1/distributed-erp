package com.example.demo.user.domain;

import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class User extends BaseEntity implements UserDetails {
	public static final int NAME_MAX_SIZE = 128;
	public static final int PASSWORD_MIN_SIZE = 8;
	public static final int PASSWORD_MAX_SIZE = 32;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "email", nullable = false)
	private String email;

	private @Nullable String password;

	@Column(name = "profile_icon_url")
	private @Nullable String profileIconUrl;

	private String role = "USER";

	public User() {
	}

	public User(String name, String email, @Nullable String password) {
		this.name = name;
		this.email = email;
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getRole() {
		return role;
	}

	public String getProfileIconUrl() {
		return profileIconUrl;
	}

	public void setProfileIconUrl(String profileIconUrl) {
		this.profileIconUrl = profileIconUrl;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@Override
	public String getUsername() {
		return email;
	}
}
