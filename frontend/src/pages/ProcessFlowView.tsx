import React, { useState, useEffect, useRef } from 'react';
import { spaceApi } from '../services/spaceApi';
import type { BoardTaskDto, CommitHistoryDto } from '../services/spaceApi';
import { useAuthStore } from '../store/authStore';
import {
    CheckCircle2,
    Circle,
    Clock,
    GitBranch,
    Plus,
    X,
    Loader2,
    AlertTriangle,
    Rocket,
    Sparkles,
    ArrowRight,
} from 'lucide-react';

// ─────────────────────────────────────────────────────────────────────────────
// 커밋 필터링 — Major(핵심) 커밋만 추출
// ─────────────────────────────────────────────────────────────────────────────

const MAJOR_PATTERNS = [
    /^feat(\(.+\))?:/i,
    /^feature(\(.+\))?:/i,
    /^✨/,
    /^💥/,
    /^BREAKING CHANGE/i,
    /^Add\s+/i,           // "Add xxx feature" 형태
    /^Implement\s+/i,
    /^Release\s+/i,
    /^Launch\s+/i,
];

/** 노이즈 커밋 패턴 — 이 패턴에 매칭되면 무조건 숨김 */
const NOISE_PATTERNS = [
    /^fix(\(.+\))?:/i,
    /^docs(\(.+\))?:/i,
    /^chore(\(.+\))?:/i,
    /^style(\(.+\))?:/i,
    /^refactor(\(.+\))?:/i,
    /^test(\(.+\))?:/i,
    /^ci(\(.+\))?:/i,
    /^build(\(.+\))?:/i,
    /^perf(\(.+\))?:/i,
    /^revert(\(.+\))?:/i,
    /^Merge (branch|pull request)/i,
    /^Update\s+(README|docs|\.md)/i,
    /^Initial commit/i,
    /^wip:/i,
];

function isMajorCommit(message: string): boolean {
    const msg = message.trim();
    // 노이즈면 무조건 false
    if (NOISE_PATTERNS.some(p => p.test(msg))) return false;
    // Major 패턴 매칭
    return MAJOR_PATTERNS.some(p => p.test(msg));
}

/** 커밋 목록에서 Major 커밋만 추출 (최신순) */
function filterMajorCommits(commits: CommitHistoryDto[]): CommitHistoryDto[] {
    return commits.filter(c => isMajorCommit(c.title));
}

// ─────────────────────────────────────────────────────────────────────────────
// 날짜 포맷
// ─────────────────────────────────────────────────────────────────────────────

function formatDate(dateStr: string): string {
    try {
        return new Date(dateStr).toLocaleDateString('ko-KR', {
            month: 'short',
            day: 'numeric',
        });
    } catch {
        return dateStr;
    }
}

/** 커밋 메시지에서 제목만 추출 (prefix 제거) */
function cleanTitle(message: string): string {
    return message
        .replace(/^(feat|feature|✨|💥|BREAKING CHANGE|Add|Implement|Release|Launch)(\(.+?\))?:\s*/i, '')
        .split('\n')[0]
        .trim();
}

// ─────────────────────────────────────────────────────────────────────────────
// 마일스톤 카드 색상 팔레트 (순환)
// ─────────────────────────────────────────────────────────────────────────────

const MILESTONE_PALETTES = [
    { bg: 'from-violet-500 to-indigo-600',   badge: 'bg-violet-100 text-violet-700', ring: 'ring-violet-300',  dot: 'bg-violet-500', line: 'bg-violet-200' },
    { bg: 'from-sky-500 to-cyan-600',        badge: 'bg-sky-100 text-sky-700',       ring: 'ring-sky-300',     dot: 'bg-sky-500',    line: 'bg-sky-200'    },
    { bg: 'from-emerald-500 to-teal-600',    badge: 'bg-emerald-100 text-emerald-700', ring: 'ring-emerald-300', dot: 'bg-emerald-500', line: 'bg-emerald-200' },
    { bg: 'from-amber-500 to-orange-500',    badge: 'bg-amber-100 text-amber-700',   ring: 'ring-amber-300',   dot: 'bg-amber-500',  line: 'bg-amber-200'  },
    { bg: 'from-rose-500 to-pink-600',       badge: 'bg-rose-100 text-rose-700',     ring: 'ring-rose-300',    dot: 'bg-rose-500',   line: 'bg-rose-200'   },
];

// ─────────────────────────────────────────────────────────────────────────────
// Milestone Node (가로 타임라인 카드)
// ─────────────────────────────────────────────────────────────────────────────

function MilestoneNode({
    commit,
    index,
    isLast,
}: {
    commit: CommitHistoryDto;
    index: number;
    isLast: boolean;
}) {
    const palette = MILESTONE_PALETTES[index % MILESTONE_PALETTES.length];
    const stepNum = index + 1;
    const title = cleanTitle(commit.title);

    return (
        <div className="flex items-center flex-shrink-0">
            {/* ── 카드 ── */}
            <div className="flex flex-col items-center w-52">
                {/* Step 뱃지 */}
                <div className={`mb-3 px-3 py-1 rounded-full text-[11px] font-bold tracking-wider uppercase
                    ${palette.badge} ring-1 ${palette.ring}`}>
                    Step {stepNum}
                </div>

                {/* 아이콘 원 */}
                <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${palette.bg}
                    flex items-center justify-center shadow-lg mb-3 ring-4 ring-white`}>
                    <Sparkles className="w-6 h-6 text-white" />
                </div>

                {/* 커넥터 도트 */}
                <div className={`w-2.5 h-2.5 rounded-full ${palette.dot} mb-2 ring-2 ring-white shadow`} />

                {/* 텍스트 카드 */}
                <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-3 w-full
                    hover:shadow-md hover:border-gray-200 transition-all duration-200 cursor-default">
                    <p className="text-[13px] font-semibold text-gray-800 leading-snug text-center line-clamp-2 min-h-[36px]">
                        {title || commit.title}
                    </p>
                    <div className="flex items-center justify-center gap-1.5 mt-2.5 pt-2.5 border-t border-gray-50">
                        <img
                            src={`https://i.pravatar.cc/40?u=${commit.assignee}`}
                            alt={commit.assignee}
                            className="w-4 h-4 rounded-full"
                        />
                        <span className="text-[10px] text-gray-400 font-medium truncate max-w-[80px]">
                            {commit.assignee}
                        </span>
                        <span className="text-[10px] text-gray-300 ml-auto flex-shrink-0">
                            {formatDate(commit.commitDate)}
                        </span>
                    </div>
                </div>
            </div>

            {/* ── 연결선 ── */}
            {!isLast && (
                <div className="flex items-center mx-2 flex-shrink-0">
                    <div className={`h-0.5 w-10 ${palette.line}`} />
                    <ArrowRight className="w-4 h-4 text-gray-300 -ml-1" />
                </div>
            )}
        </div>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// TodoCard
// ─────────────────────────────────────────────────────────────────────────────

function TodoCard({
    task,
    onCheck,
    onDelete,
}: {
    task: BoardTaskDto;
    onCheck: (id: number) => void;
    onDelete: (id: number) => void;
}) {
    const [leaving, setLeaving] = useState(false);

    const handleCheck = () => {
        setLeaving(true);
        setTimeout(() => onCheck(task.id), 320);
    };

    return (
        <div
            className={`group bg-white rounded-xl border border-gray-100 p-3 flex items-start gap-3
                shadow-sm hover:shadow-md hover:border-indigo-200
                transition-all duration-300 ease-in-out
                ${leaving ? 'opacity-0 translate-x-5 scale-95' : 'opacity-100 translate-x-0 scale-100'}`}
        >
            {/* 체크버튼 */}
            <button
                id={`task-check-${task.id}`}
                onClick={handleCheck}
                title="In Progress로 이동"
                className="mt-0.5 flex-shrink-0 w-5 h-5 rounded-full border-2 border-gray-200
                    hover:border-indigo-400 hover:bg-indigo-50 transition-all duration-200
                    flex items-center justify-center group/btn"
            >
                <div className="w-2 h-2 rounded-full bg-transparent group-hover/btn:bg-indigo-400 transition-colors" />
            </button>

            <p className="flex-1 text-sm text-gray-700 leading-relaxed pt-0.5 break-words">{task.title}</p>

            <button
                id={`task-delete-${task.id}`}
                onClick={() => onDelete(task.id)}
                title="삭제"
                className="flex-shrink-0 opacity-0 group-hover:opacity-100 text-gray-300
                    hover:text-red-400 transition-all duration-150"
            >
                <X className="w-3.5 h-3.5" />
            </button>
        </div>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// InProgressCard
// ─────────────────────────────────────────────────────────────────────────────

function InProgressCard({
    task,
    onDelete,
}: {
    task: BoardTaskDto;
    onDelete: (id: number) => void;
}) {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        const t = setTimeout(() => setVisible(true), 40);
        return () => clearTimeout(t);
    }, []);

    return (
        <div
            className={`group bg-gradient-to-r from-indigo-50 to-violet-50 rounded-xl
                border border-indigo-100 p-3 flex items-start gap-3 shadow-sm
                transition-all duration-300 ease-out
                ${visible ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-3 scale-95'}`}
        >
            <Clock className="w-4 h-4 text-indigo-400 flex-shrink-0 mt-0.5 animate-pulse" />
            <p className="flex-1 text-sm text-indigo-800 font-medium leading-relaxed break-words">{task.title}</p>
            <button
                id={`inprogress-delete-${task.id}`}
                onClick={() => onDelete(task.id)}
                title="삭제"
                className="flex-shrink-0 opacity-0 group-hover:opacity-100 text-indigo-200
                    hover:text-red-400 transition-all duration-150"
            >
                <X className="w-3.5 h-3.5" />
            </button>
        </div>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Component
// ─────────────────────────────────────────────────────────────────────────────

export function ProcessFlowView() {
    const { user } = useAuthStore();
    const teamCode = user?.teamCode ?? null;

    // ── 커밋 상태 ──
    const [majorCommits, setMajorCommits] = useState<CommitHistoryDto[]>([]);
    const [isCommitLoading, setIsCommitLoading] = useState(false);
    const [isSyncing, setIsSyncing] = useState(false);
    const [commitError, setCommitError] = useState<string | null>(null);

    // ── 태스크 상태 ──
    const [tasks, setTasks] = useState<BoardTaskDto[]>([]);
    const [isTaskLoading, setIsTaskLoading] = useState(false);
    const [newTaskText, setNewTaskText] = useState('');
    const [isAdding, setIsAdding] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);
    const timelineRef = useRef<HTMLDivElement>(null);

    /** 커밋 로드: DB에 없으면 GitHub에서 자동 동기화 */
    const loadCommits = async (code: string, forceSync = false) => {
        setIsCommitLoading(true);
        setCommitError(null);
        try {
            let data: CommitHistoryDto[];

            if (forceSync) {
                // 수동 재동기화 버튼
                setIsSyncing(true);
                data = await spaceApi.syncCommits(code);
                setIsSyncing(false);
            } else {
                // 1차: DB 조회 (백엔드가 비어있으면 온디맨드 sync 후 반환)
                data = await spaceApi.getCommits(code);
            }

            const majors = filterMajorCommits(data).reverse();
            setMajorCommits(majors);
        } catch (e: any) {
            setIsSyncing(false);
            setCommitError(e?.message ?? '커밋 내역을 불러올 수 없습니다.');
        } finally {
            setIsCommitLoading(false);
        }
    };

    // ── 데이터 로드 (teamCode 변경 시 자동 실행) ──
    useEffect(() => {
        if (!teamCode) return;

        // 커밋: 스페이스 전환마다 재로드
        loadCommits(teamCode);

        // 태스크
        (async () => {
            setIsTaskLoading(true);
            try {
                const data = await spaceApi.getTasks(teamCode);
                setTasks(data);
            } catch (e) {
                console.error('Tasks load failed:', e);
            } finally {
                setIsTaskLoading(false);
            }
        })();
    }, [teamCode]);

    // ── 태스크 핸들러 ──
    const handleAddTask = async () => {
        if (!teamCode || !newTaskText.trim()) return;
        setIsAdding(true);
        try {
            const created = await spaceApi.createTask(teamCode, newTaskText.trim());
            setTasks(prev => [created, ...prev]);
            setNewTaskText('');
            inputRef.current?.focus();
        } catch (e) {
            console.error('Create task failed:', e);
        } finally {
            setIsAdding(false);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') handleAddTask();
    };

    const handleCheck = async (taskId: number) => {
        if (!teamCode) return;
        try {
            const updated = await spaceApi.updateTaskStatus(teamCode, taskId, 'IN_PROGRESS');
            setTasks(prev => prev.map(t => (t.id === taskId ? updated : t)));
        } catch (e) {
            console.error('Status update failed:', e);
        }
    };

    const handleDelete = async (taskId: number) => {
        try {
            await spaceApi.deleteTask(taskId);
            setTasks(prev => prev.filter(t => t.id !== taskId));
        } catch (e) {
            console.error('Delete failed:', e);
        }
    };

    const todoTasks       = tasks.filter(t => t.status === 'TODO');
    const inProgressTasks = tasks.filter(t => t.status === 'IN_PROGRESS');

    // ── 팀 미소속 ──
    if (!teamCode) {
        return (
            <div className="flex flex-col items-center justify-center h-full bg-gradient-to-br from-slate-50 to-gray-100 gap-4">
                <AlertTriangle className="w-12 h-12 text-amber-400" />
                <p className="text-gray-500 font-medium">팀 스페이스에 참여한 후 이용할 수 있습니다.</p>
            </div>
        );
    }

    return (
        <div className="flex flex-col h-full bg-[#F5F6FA] overflow-hidden">

            {/* ================================================================
                TOP — 브랜치 Flow View (가로 타임라인 로드맵)
            ================================================================ */}
            <div className="flex-shrink-0 bg-white border-b border-gray-100 shadow-sm"
                style={{ height: '54%' }}>

                {/* 헤더 */}
                <div className="flex items-center justify-between px-6 pt-5 pb-3">
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-violet-500 to-indigo-600
                            flex items-center justify-center shadow-md">
                            <Rocket className="w-4 h-4 text-white" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-gray-900 leading-none">
                                프로젝트 로드맵
                            </h2>
                            <p className="text-[11px] text-gray-400 mt-0.5">
                                핵심 기능 커밋만 표시 &nbsp;·&nbsp;
                                <span className="font-mono font-semibold text-gray-500">{teamCode}</span>
                            </p>
                        </div>
                    </div>

                    {/* 범례 + 새로고침 */}
                    <div className="flex items-center gap-2">
                        <div className="flex items-center gap-1.5 bg-gray-50 border border-gray-100
                            rounded-xl px-3 py-1.5">
                            <Sparkles className="w-3.5 h-3.5 text-violet-500" />
                            <span className="text-[11px] text-gray-500 font-medium">feat · feature · 핵심 기능만</span>
                        </div>
                        <button
                            id="commit-refresh-btn"
                            onClick={() => teamCode && loadCommits(teamCode, true)}
                            disabled={isCommitLoading}
                            title="GitHub에서 최신 커밋 재동기화"
                            className="w-8 h-8 flex items-center justify-center rounded-xl border border-gray-200
                                bg-gray-50 hover:bg-violet-50 hover:border-violet-200 text-gray-400
                                hover:text-violet-600 disabled:opacity-40 transition-all duration-200"
                        >
                            <Loader2 className={`w-4 h-4 ${isCommitLoading ? 'animate-spin text-violet-500' : ''}`} />
                        </button>
                    </div>
                </div>

                {/* 타임라인 (가로 스크롤) */}
                <div
                    ref={timelineRef}
                    className="flex-1 overflow-x-auto overflow-y-hidden px-6 pb-5 pt-2
                        scrollbar-thin scrollbar-thumb-gray-200 scrollbar-track-transparent"
                    style={{ height: 'calc(100% - 68px)' }}
                >
                    {isCommitLoading ? (
                        <div className="flex items-center justify-center h-full gap-3 text-gray-400">
                            <Loader2 className="w-6 h-6 animate-spin text-violet-400" />
                            <span className="text-sm">
                                {isSyncing ? 'GitHub에서 커밋 동기화 중...' : '커밋 내역 불러오는 중...'}
                            </span>
                        </div>

                    ) : commitError ? (
                        <div className="flex flex-col items-center justify-center h-full gap-3">
                            <AlertTriangle className="w-8 h-8 text-amber-400" />
                            <p className="text-sm text-gray-400">{commitError}</p>
                            <button
                                id="commit-retry-btn"
                                onClick={() => teamCode && loadCommits(teamCode, true)}
                                className="flex items-center gap-2 px-4 py-2 bg-violet-600 hover:bg-violet-700
                                    text-white text-xs font-semibold rounded-xl transition-colors shadow-sm"
                            >
                                <Loader2 className="w-3.5 h-3.5" />
                                GitHub에서 재동기화
                            </button>
                        </div>

                    ) : majorCommits.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-full gap-3 text-gray-300">
                            <GitBranch className="w-10 h-10" />
                            <p className="text-sm text-gray-400">표시할 핵심 커밋이 없습니다.</p>
                            <p className="text-xs text-gray-300">
                                <code className="bg-gray-100 px-1 rounded text-gray-400">feat:</code> 커밋이 있어야 로드맵에 표시됩니다.
                            </p>
                            <button
                                id="commit-sync-btn"
                                onClick={() => teamCode && loadCommits(teamCode, true)}
                                className="mt-1 flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-violet-50
                                    hover:text-violet-700 text-gray-500 text-xs font-semibold
                                    rounded-xl border border-gray-200 hover:border-violet-200
                                    transition-all duration-200"
                            >
                                <Loader2 className="w-3.5 h-3.5" />
                                GitHub에서 다시 동기화
                            </button>
                        </div>

                    ) : (
                        /* 타임라인 레일 */
                        <div className="relative flex items-start h-full">
                            {/* 배경 레일 선 */}
                            <div className="absolute left-8 right-8 top-[3.4rem] h-0.5
                                bg-gradient-to-r from-violet-100 via-indigo-100 to-gray-100 rounded-full" />

                            {/* 노드들 */}
                            <div className="flex items-start gap-0 relative z-10">
                                {majorCommits.map((commit, idx) => (
                                    <MilestoneNode
                                        key={commit.id}
                                        commit={commit}
                                        index={idx}
                                        isLast={idx === majorCommits.length - 1}
                                    />
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* ================================================================
                BOTTOM — 할 일 보드 (Todo + In Progress)
            ================================================================ */}
            <div className="flex-1 flex overflow-hidden min-h-0">

                {/* ── Todo 열 ── */}
                <div className="flex-1 flex flex-col border-r border-gray-200 overflow-hidden bg-white/60">

                    {/* 열 헤더 */}
                    <div className="flex-shrink-0 flex items-center justify-between px-5 py-3
                        border-b border-gray-100 bg-white">
                        <div className="flex items-center gap-2">
                            <Circle className="w-3.5 h-3.5 text-gray-400" />
                            <span className="text-xs font-bold text-gray-600 uppercase tracking-widest">Todo</span>
                        </div>
                        <span className="bg-gray-100 text-gray-500 text-[10px] font-bold px-2 py-0.5 rounded-full">
                            {todoTasks.length}
                        </span>
                    </div>

                    {/* 입력창 */}
                    <div className="flex-shrink-0 px-4 pt-3 pb-2">
                        <div className="flex gap-2 items-center bg-white border border-gray-200
                            rounded-xl px-3 py-2 focus-within:border-indigo-300 focus-within:ring-2
                            focus-within:ring-indigo-50 transition-all shadow-sm">
                            <input
                                id="todo-input"
                                ref={inputRef}
                                type="text"
                                value={newTaskText}
                                onChange={e => setNewTaskText(e.target.value)}
                                onKeyDown={handleKeyDown}
                                placeholder="새 할 일을 입력하고 Enter..."
                                className="flex-1 text-sm text-gray-800 placeholder-gray-300 bg-transparent outline-none"
                            />
                            <button
                                id="todo-add-btn"
                                onClick={handleAddTask}
                                disabled={isAdding || !newTaskText.trim()}
                                className="flex-shrink-0 w-7 h-7 bg-indigo-600 hover:bg-indigo-700
                                    disabled:bg-gray-200 text-white rounded-lg flex items-center justify-center
                                    transition-colors shadow-sm"
                            >
                                {isAdding
                                    ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                    : <Plus className="w-3.5 h-3.5" />
                                }
                            </button>
                        </div>
                    </div>

                    {/* 카드 목록 */}
                    <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-2 min-h-0">
                        {isTaskLoading ? (
                            <div className="flex justify-center py-8">
                                <Loader2 className="w-5 h-5 text-gray-300 animate-spin" />
                            </div>
                        ) : todoTasks.length === 0 ? (
                            <div className="flex flex-col items-center justify-center py-10 gap-2
                                border-2 border-dashed border-gray-100 rounded-2xl mt-1 text-gray-300">
                                <CheckCircle2 className="w-7 h-7" />
                                <p className="text-xs">할 일을 입력해 주세요</p>
                            </div>
                        ) : (
                            todoTasks.map(task => (
                                <TodoCard
                                    key={task.id}
                                    task={task}
                                    onCheck={handleCheck}
                                    onDelete={handleDelete}
                                />
                            ))
                        )}
                    </div>
                </div>

                {/* ── In Progress 열 ── */}
                <div className="flex-1 flex flex-col overflow-hidden bg-white/40">

                    {/* 열 헤더 */}
                    <div className="flex-shrink-0 flex items-center justify-between px-5 py-3
                        border-b border-gray-100 bg-white">
                        <div className="flex items-center gap-2">
                            <Clock className="w-3.5 h-3.5 text-indigo-500" />
                            <span className="text-xs font-bold text-indigo-600 uppercase tracking-widest">In Progress</span>
                        </div>
                        <span className="bg-indigo-50 text-indigo-500 text-[10px] font-bold px-2 py-0.5 rounded-full">
                            {inProgressTasks.length}
                        </span>
                    </div>

                    {/* 가이드 */}
                    <div className="flex-shrink-0 px-4 pt-3 pb-2">
                        <div className="flex items-center gap-2 bg-indigo-50/60 border border-indigo-100
                            rounded-xl px-3 py-2 text-[11px] text-indigo-400">
                            <Circle className="w-3 h-3" />
                            <span>Todo의 원형 버튼을 클릭하면 이동합니다</span>
                        </div>
                    </div>

                    {/* 카드 목록 */}
                    <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-2 min-h-0">
                        {inProgressTasks.length === 0 ? (
                            <div className="flex flex-col items-center justify-center py-10 gap-2
                                border-2 border-dashed border-indigo-100/60 rounded-2xl mt-1 text-indigo-200">
                                <Clock className="w-7 h-7" />
                                <p className="text-xs">진행 중인 작업이 없습니다</p>
                            </div>
                        ) : (
                            inProgressTasks.map(task => (
                                <InProgressCard
                                    key={task.id}
                                    task={task}
                                    onDelete={handleDelete}
                                />
                            ))
                        )}
                    </div>
                </div>

            </div>
        </div>
    );
}