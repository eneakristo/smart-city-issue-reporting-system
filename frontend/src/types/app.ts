export type UserRole = 'citizen' | 'administrator' | 'system_admin';
export type AssignableUserRole = UserRole;
export type CityZone = 'ZONE_1' | 'ZONE_2';

export type UserProfile = {
  id: string;
  fullName: string;
  email: string;
  phoneNumber: string | null;
  bio: string | null;
  profilePictureUrl: string | null;
  role: UserRole;
  assignedZone?: CityZone | null;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  userId: string;
  fullName: string;
  email: string;
  phoneNumber: string | null;
  bio: string | null;
  profilePictureUrl: string | null;
  role: string;
  assignedZone?: CityZone | null;
};

export type AuthSession = {
  accessToken: string;
  user: UserProfile;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = {
  fullName: string;
  email: string;
  password: string;
};

export type ForgotPasswordPayload = {
  email: string;
};

export type ResetPasswordPayload = {
  token: string;
  password: string;
};

export type UpdateProfilePayload = {
  fullName: string;
  phoneNumber?: string;
  bio?: string;
  profilePictureUrl?: string;
};

export type ChangePasswordPayload = {
  currentPassword: string;
  newPassword: string;
};

export type ManagedUser = {
  id: string;
  fullName: string;
  email: string;
  role: string;
  assignedZone: CityZone | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateManagedUserPayload = {
  fullName: string;
  email: string;
  password: string;
  role: AssignableUserRole;
  assignedZone?: CityZone | null;
};

export type UpdateManagedUserRolePayload = {
  role: AssignableUserRole;
  assignedZone?: CityZone | null;
};

export type AuthFormValues = {
  fullName?: string;
  email: string;
  password: string;
  confirmPassword?: string;
};

export type CreateIssuePayload = {
  title: string;
  description: string;
  category: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  locationText: string;
  latitude: number | null;
  longitude: number | null;
};

export type IssueImage = {
  id: string;
  publicUrl: string;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  displayOrder: number;
};

export type IssueStatus =
  | 'REPORTED'
  | 'UNDER_REVIEW'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'REJECTED';

export type IssueSortOption = 'NEWEST' | 'MOST_VOTED' | 'HIGHEST_PRIORITY';

export type IssueSummary = {
  issueId: string;
  title: string;
  category: string;
  severity: string;
  status: IssueStatus;
  issueType: 'PRIMARY' | 'DUPLICATE';
  locationText: string;
  zone: CityZone | null;
  thumbnailUrl: string | null;
  voteCount: number;
  currentUserHasVoted: boolean;
  priorityScore: number | null;
  parentIssueId: string | null;
  parentIssueTitle: string | null;
  createdAt: string;
};

export type AdminIssueNotification = {
  issueId: string;
  title: string;
  category: string;
  status: IssueStatus;
  issueType: 'PRIMARY' | 'DUPLICATE';
  reporterName: string;
  createdAt: string;
};

export type IssueDetails = {
  issueId: string;
  title: string;
  description: string;
  category: string;
  severity: string;
  status: IssueStatus;
  issueType: 'PRIMARY' | 'DUPLICATE';
  locationText: string;
  latitude: number | null;
  longitude: number | null;
  zone: CityZone | null;
  priorityScore: number | null;
  voteCount: number;
  currentUserHasVoted: boolean;
  reporterId: string;
  reporterName: string;
  parentIssueId: string | null;
  linkedDuplicateCount: number;
  images: IssueImage[];
  resolutionImages: IssueImage[];
  statusHistory: IssueStatusHistoryEntry[];
  createdAt: string;
  updatedAt: string;
};

export type IssueStatusHistoryEntry = {
  historyId: string;
  oldStatus: IssueStatus | null;
  newStatus: IssueStatus;
  changedByUserId: string;
  changedByName: string;
  note: string | null;
  changedAt: string;
};

export type IssueListFilters = {
  keyword?: string;
  category?: string;
  status?: IssueStatus | '';
  locationText?: string;
  sort?: IssueSortOption;
};

export type VoteIssueResponse = {
  issueId: string;
  voteCount: number;
  currentUserHasVoted: boolean;
  action: 'added' | 'removed' | string;
  priorityScore: number | null;
};

export type CreateIssueResponse = {
  issueId: string;
  title: string;
  category: string;
  severity: string;
  status: string;
  issueType: string;
  locationText: string;
  zone: CityZone | null;
  reporterId: string;
  duplicateReviewRequired: boolean;
  linkedParentIssueId: string | null;
  duplicateCandidates: Array<{
    issueId: string;
    title: string;
    locationText: string;
    confidence: number;
    matchReason: string;
  }>;
  images: IssueImage[];
  createdAt: string;
};

export type AdminDuplicateReviewDecision =
  | 'PENDING'
  | 'CONFIRMED_DUPLICATE'
  | 'REJECTED_DUPLICATE';

export type AdminDuplicateOriginalIssue = {
  issueId: string;
  title: string;
  category: string;
  status: IssueStatus;
  issueType: 'PRIMARY' | 'DUPLICATE';
  locationText: string;
};

export type AdminDuplicateReview = {
  reviewId: string;
  duplicateIssueId: string;
  reportedTitle: string;
  category: string;
  locationText: string;
  status: IssueStatus;
  issueType: 'PRIMARY' | 'DUPLICATE';
  images: IssueImage[];
  linkedOriginalIssue: AdminDuplicateOriginalIssue | null;
  duplicateConfidence: number | null;
  matchReason: string | null;
  reviewDecision: AdminDuplicateReviewDecision;
  reviewNotes: string | null;
  reviewedByUserId: string | null;
  reviewedByName: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminManageableStatus = 'REPORTED' | 'CONFIRMED' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';

export type AdminStatusUpdatePayload = {
  status: AdminManageableStatus | 'OPEN';
  note?: string;
  resolutionImage?: File;
};

export type AdminSeverity = 'LOW' | 'MEDIUM' | 'HIGH';

export type AdminIssueSeverityResponse = {
  issueId: string;
  title: string;
  reportedSeverity: AdminSeverity;
  verifiedSeverity: AdminSeverity | null;
  effectiveSeverity: AdminSeverity;
  priorityScore: number | null;
};
