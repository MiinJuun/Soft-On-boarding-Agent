import { useAuthStore } from '../store/authStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// 공통 Fetch 옵션 생성기 (JWT 포함)
const getAuthHeaders = () => {
  const token = useAuthStore.getState().token;
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

export interface CreateSpaceRequest {
  name: string;
  repoUrl: string;
  jobRole: string;
}

export interface CreateSpaceResponse {
  spaceId: number;
  teamCode: string;
}

// ---- Commit DTO ----
export interface CommitHistoryDto {
  id: string;         // commitSha
  title: string;      // message
  assignee: string;   // author
  commitDate: string;
}

// ---- BoardTask DTO ----
export type BoardTaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface BoardTaskDto {
  id: number;
  spaceId: number;
  title: string;
  status: BoardTaskStatus;
  assignee: string | null;
  label: string | null;
  createdAt: string;
}

export const spaceApi = {
  // ======================================================================
  // Space
  // ======================================================================

  createSpace: async (data: CreateSpaceRequest): Promise<CreateSpaceResponse> => {
    const response = await fetch(`${API_BASE_URL}/api/spaces`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '팀 스페이스 생성에 실패했습니다.');
    }

    return response.json();
  },

  joinSpace: async (teamCode: string, jobRole: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/api/spaces/join`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ teamCode, jobRole }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '팀 스페이스 참여에 실패했습니다.');
    }
  },

  leaveSpace: async (): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/api/spaces/leave`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '팀 탈퇴에 실패했습니다.');
    }
  },

  // ======================================================================
  // Commits — 팀 코드 기반으로 격리된 커밋 목록 조회
  // ======================================================================

  getCommits: async (teamCode: string): Promise<CommitHistoryDto[]> => {
    const response = await fetch(`${API_BASE_URL}/api/spaces/${teamCode}/commits`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '커밋 내역을 불러오는데 실패했습니다.');
    }

    return response.json();
  },

  /**
   * 해당 팀의 GitHub repo_url 기반으로 커밋을 강제 재동기화합니다.
   * 스페이스 전환 시 DB에 데이터가 없을 때 호출합니다.
   */
  syncCommits: async (teamCode: string): Promise<CommitHistoryDto[]> => {
    const response = await fetch(`${API_BASE_URL}/api/spaces/${teamCode}/commits/sync`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '커밋 동기화에 실패했습니다.');
    }

    return response.json();
  },

  // ======================================================================
  // BoardTask CRUD — 모든 요청에 teamCode 포함하여 팀 격리 보장
  // ======================================================================

  /** 태스크 목록 조회 */
  getTasks: async (teamCode: string): Promise<BoardTaskDto[]> => {
    const response = await fetch(`${API_BASE_URL}/api/spaces/${teamCode}/tasks`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '태스크를 불러오는데 실패했습니다.');
    }

    return response.json();
  },

  /** 새 태스크 생성 (수기 입력 메모장용) */
  createTask: async (teamCode: string, title: string): Promise<BoardTaskDto> => {
    const response = await fetch(`${API_BASE_URL}/api/spaces/${teamCode}/tasks`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ title, status: 'TODO' }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '태스크 생성에 실패했습니다.');
    }

    return response.json();
  },

  /** 태스크 상태만 변경 (체크박스 클릭 → IN_PROGRESS) */
  updateTaskStatus: async (
    teamCode: string,
    taskId: number,
    status: BoardTaskStatus
  ): Promise<BoardTaskDto> => {
    const response = await fetch(
      `${API_BASE_URL}/api/spaces/${teamCode}/tasks/${taskId}/status`,
      {
        method: 'PATCH',
        headers: getAuthHeaders(),
        body: JSON.stringify({ status }),
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '태스크 상태 변경에 실패했습니다.');
    }

    return response.json();
  },

  /** 태스크 삭제 */
  deleteTask: async (taskId: number): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/api/spaces/tasks/${taskId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || '태스크 삭제에 실패했습니다.');
    }
  },
};
