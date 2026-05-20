import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

const api = axios.create({
    baseURL: BASE_URL,
    withCredentials: true,
});

// attach JWT from localStorage automatically
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// ─── Types ────────────────────────────────────────────────────────────────────

export type BoardTaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface BoardTask {
    id: number;
    spaceId: number;
    title: string;
    status: BoardTaskStatus;
    assignee: string | null;
    label: string | null;
    createdAt: string;
}

export interface CreateBoardTaskRequest {
    title: string;
    status?: BoardTaskStatus;
    assignee?: string;
    label?: string;
}

export interface CommitHistory {
    id: number;
    repoName: string;
    commitSha: string;
    message: string;
    author: string;
    commitDate: string;
}

// ─── Task CRUD ────────────────────────────────────────────────────────────────

/** 특정 팀의 모든 태스크 조회 */
export const getTasks = (teamCode: string): Promise<BoardTask[]> =>
    api.get<BoardTask[]>(`/api/spaces/${teamCode}/tasks`).then((r) => r.data);

/** 새 태스크 생성 */
export const createTask = (
    teamCode: string,
    body: CreateBoardTaskRequest,
): Promise<BoardTask> =>
    api.post<BoardTask>(`/api/spaces/${teamCode}/tasks`, body).then((r) => r.data);

/** 태스크 수정 (title / status / assignee / label 부분 업데이트) */
export const updateTask = (
    taskId: number,
    body: Partial<CreateBoardTaskRequest>,
): Promise<BoardTask> =>
    api.put<BoardTask>(`/api/spaces/tasks/${taskId}`, body).then((r) => r.data);

/** 태스크 삭제 */
export const deleteTask = (taskId: number): Promise<void> =>
    api.delete(`/api/spaces/tasks/${taskId}`).then(() => undefined);

// ─── Commit History ───────────────────────────────────────────────────────────

/** 팀 커밋 내역 조회 */
export const getCommits = (teamCode: string): Promise<CommitHistory[]> =>
    api.get<CommitHistory[]>(`/api/spaces/${teamCode}/commits`).then((r) => r.data);
