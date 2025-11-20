/**
 * User model interface.
 *
 * Represents a user in the system.
 */
export interface User {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  active: boolean;
  emailVerified: boolean;
  roles: UserRole[];
  createdAt?: string;
  updatedAt?: string;
  lastLoginAt?: string;
}

/**
 * User role enum.
 */
export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
  TECH_LEAD = 'TECH_LEAD',
  MANAGER = 'MANAGER',
  SYSTEM = 'SYSTEM',
}

/**
 * Create user request DTO.
 */
export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  roles?: UserRole[];
}

/**
 * Update user request DTO.
 */
export interface UpdateUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  active?: boolean;
  roles?: UserRole[];
}

/**
 * Paginated response wrapper.
 */
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
