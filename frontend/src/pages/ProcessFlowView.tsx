import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
    CheckCircle2,
    Circle,
    Clock,
    GitBranch,
    LayoutGrid,
    Plus,
    Pencil,
    Trash2,
    X,
    Check,
    GripVertical,
    GitCommitHorizontal,
    Loader2,
} from 'lucide-react';
import {
    getTasks,
    createTask,
    updateTask,
    deleteTask,
    getCommits,
    BoardTask,
    BoardTaskStatus,
    CommitHistory,
} from '../services/spaceApi';

// ─── Types ────────────────────────────────────────────────────────────────────

type ReleaseType = 'release' | 'hotfix';
type IssueStatus = 'Todo' | 'In Progress' | 'Done';
type LabelType = 'Frontend' | 'Backend' | 'DevOps' | 'Design' | '';

interface Release {
    id: number;
    name: string;
    date: string;
    type: ReleaseType;
    completedTasks: number;
    totalTasks: number;
}

// form state used when creating or editing a task
interface TaskFormState {
    title: string;
    assignee: string;
    label: LabelType;
}

// ─── Constants ────────────────────────────────────────────────────────────────

const LABELS: LabelType[] = ['Frontend', 'Backend', 'DevOps', 'Design'];

const DUMMY_RELEASES: Release[] = [
    { id: 1, name: 'v1.0.0 (MVP)',    date: 'May 10', type: 'release', completedTasks: 15, totalTasks: 15 },
    { id: 2, name: 'Hotfix-Auth',     date: 'May 12', type: 'hotfix',  completedTasks: 2,  totalTasks: 2  },
    { id: 3, name: 'v1.1.0 (Beta)',   date: 'May 15', type: 'release', completedTasks: 8,  totalTasks: 12 },
    { id: 4, name: 'v1.2.0 (RC)',     date: 'May 20', type: 'release', completedTasks: 1,  totalTasks: 20 },
];

// Read teamCode from localStorage (set during space creation / join)
const getTeamCode = (): string =>
    localStorage.getItem('teamCode') ?? 'DEMO_TEAM';

// ─── Style Helpers ────────────────────────────────────────────────────────────

const getLabelStyle = (label: string | null) => {
    switch (label) {
        case 'Frontend': return 'bg-blue-50 text-blue-700 border-blue-200';
        case 'Backend':  return 'bg-orange-50 text-orange-700 border-orange-200';
        case 'DevOps':   return 'bg-purple-50 text-purple-700 border-purple-200';
        case 'Design':   return 'bg-pink-50 text-pink-700 border-pink-200';
        default:         return 'bg-gray-100 text-gray-500 border-gray-200';
    }
};

const getStatusIcon = (status: IssueStatus) => {
    switch (status) {
        case 'Todo':        return <Circle        className="w-4 h-4 text-gray-400" />;
        case 'In Progress': return <Clock         className="w-4 h-4 text-amber-500" />;
        case 'Done':        return <CheckCircle2  className="w-4 h-4 text-green-500" />;
    }
};

// BoardTaskStatus (백엔드 enum) ↔ IssueStatus (UI) 매핑
const toApiStatus = (s: IssueStatus): BoardTaskStatus =>
    s === 'Todo' ? 'TODO' : s === 'In Progress' ? 'IN_PROGRESS' : 'DONE';

const toUiStatus = (s: BoardTaskStatus): IssueStatus =>
    s === 'TODO' ? 'Todo' : s === 'IN_PROGRESS' ? 'In Progress' : 'Done';

// ─── Sub-components ───────────────────────────────────────────────────────────

/** 인라인 생성 폼 */
function AddTaskForm({
    onAdd,
    onCancel,
}: {
    onAdd: (form: TaskFormState) => Promise<void>;
    onCancel: () => void;
}) {
    const [form, setForm] = useState<TaskFormState>({ title: '', assignee: '', label: '' });
    const [loading, setLoading] = useState(false);
    const titleRef = useRef<HTMLInputElement>(null);

    useEffect(() => { titleRef.current?.focus(); }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.title.trim()) return;
        setLoading(true);
        await onAdd(form);
        setLoading(false);
    };

    return (
        <form
            onSubmit={handleSubmit}
            className="bg-white rounded-xl border-2 border-blue-400 shadow-md p-3 flex flex-col gap-2"
        >
            <input
                ref={titleRef}
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="할 일을 입력하세요..."
                className="text-sm font-medium text-gray-800 w-full outline-none placeholder:text-gray-400"
            />

            <div className="flex gap-2">
                <input
                    value={form.assignee}
                    onChange={(e) => setForm({ ...form, assignee: e.target.value })}
                    placeholder="담당자"
                    className="text-xs w-full border border-gray-200 rounded-lg px-2 py-1 outline-none focus:border-blue-400"
                />
                <select
                    value={form.label}
                    onChange={(e) => setForm({ ...form, label: e.target.value as LabelType })}
                    className="text-xs border border-gray-200 rounded-lg px-2 py-1 outline-none focus:border-blue-400 bg-white"
                >
                    <option value="">라벨</option>
                    {LABELS.map((l) => <option key={l} value={l}>{l}</option>)}
                </select>
            </div>

            <div className="flex gap-2 justify-end mt-1">
                <button
                    type="button"
                    onClick={onCancel}
                    className="text-xs text-gray-500 hover:text-gray-700 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors flex items-center gap-1"
                >
                    <X className="w-3 h-3" /> 취소
                </button>
                <button
                    type="submit"
                    disabled={loading || !form.title.trim()}
                    className="text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1"
                >
                    {loading ? <Loader2 className="w-3 h-3 animate-spin" /> : <Check className="w-3 h-3" />}
                    추가
                </button>
            </div>
        </form>
    );
}

/** 인라인 수정 폼 */
function EditTaskForm({
    task,
    onSave,
    onCancel,
}: {
    task: BoardTask;
    onSave: (form: TaskFormState) => Promise<void>;
    onCancel: () => void;
}) {
    const [form, setForm] = useState<TaskFormState>({
        title: task.title,
        assignee: task.assignee ?? '',
        label: (task.label as LabelType) ?? '',
    });
    const [loading, setLoading] = useState(false);
    const titleRef = useRef<HTMLInputElement>(null);

    useEffect(() => { titleRef.current?.focus(); }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.title.trim()) return;
        setLoading(true);
        await onSave(form);
        setLoading(false);
    };

    return (
        <form
            onSubmit={handleSubmit}
            className="bg-white rounded-xl border-2 border-amber-400 shadow-md p-3 flex flex-col gap-2"
        >
            <input
                ref={titleRef}
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                className="text-sm font-medium text-gray-800 w-full outline-none border-b border-gray-200 pb-1 focus:border-amber-400"
            />
            <div className="flex gap-2">
                <input
                    value={form.assignee}
                    onChange={(e) => setForm({ ...form, assignee: e.target.value })}
                    placeholder="담당자"
                    className="text-xs w-full border border-gray-200 rounded-lg px-2 py-1 outline-none focus:border-amber-400"
                />
                <select
                    value={form.label}
                    onChange={(e) => setForm({ ...form, label: e.target.value as LabelType })}
                    className="text-xs border border-gray-200 rounded-lg px-2 py-1 outline-none focus:border-amber-400 bg-white"
                >
                    <option value="">라벨 없음</option>
                    {LABELS.map((l) => <option key={l} value={l}>{l}</option>)}
                </select>
            </div>
            <div className="flex gap-2 justify-end mt-1">
                <button
                    type="button"
                    onClick={onCancel}
                    className="text-xs text-gray-500 hover:text-gray-700 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors flex items-center gap-1"
                >
                    <X className="w-3 h-3" /> 취소
                </button>
                <button
                    type="submit"
                    disabled={loading || !form.title.trim()}
                    className="text-xs font-semibold text-white bg-amber-500 hover:bg-amber-600 disabled:opacity-50 px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1"
                >
                    {loading ? <Loader2 className="w-3 h-3 animate-spin" /> : <Check className="w-3 h-3" />}
                    저장
                </button>
            </div>
        </form>
    );
}

/** BoardTask 카드 */
function TaskCard({
    task,
    onEdit,
    onDelete,
    onMoveToInProgress,
    isDragging,
    dragHandleProps,
}: {
    task: BoardTask;
    onEdit: () => void;
    onDelete: () => void;
    onMoveToInProgress?: () => void;
    isDragging: boolean;
    dragHandleProps: React.HTMLAttributes<HTMLDivElement>;
}) {
    const [confirmDelete, setConfirmDelete] = useState(false);

    return (
        <div
            className={`bg-white p-4 rounded-xl border transition-all group
                ${isDragging
                    ? 'shadow-2xl border-blue-400 scale-[1.02] rotate-1 opacity-80'
                    : 'shadow-sm border-gray-200 hover:shadow-md hover:border-gray-300'
                }`}
        >
            {/* Top row: drag handle + label badge + action icons */}
            <div className="flex items-start justify-between mb-2">
                <div className="flex items-center gap-1.5 min-w-0">
                    {/* Drag handle */}
                    <div
                        {...dragHandleProps}
                        className="cursor-grab active:cursor-grabbing text-gray-300 hover:text-gray-500 flex-shrink-0 -ml-1"
                        title="드래그하여 이동"
                    >
                        <GripVertical className="w-4 h-4" />
                    </div>

                    {task.label ? (
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase tracking-wider flex-shrink-0 ${getLabelStyle(task.label)}`}>
                            {task.label}
                        </span>
                    ) : (
                        <span className="text-[10px] text-gray-300 italic">라벨 없음</span>
                    )}
                </div>

                {/* Action buttons */}
                <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
                    <button
                        onClick={onEdit}
                        title="수정"
                        className="p-1 rounded-md hover:bg-amber-50 text-gray-400 hover:text-amber-600 transition-colors"
                    >
                        <Pencil className="w-3.5 h-3.5" />
                    </button>

                    {confirmDelete ? (
                        <>
                            <button
                                onClick={onDelete}
                                className="px-2 py-0.5 text-[10px] font-bold bg-red-500 text-white rounded-md hover:bg-red-600 transition-colors"
                            >
                                삭제
                            </button>
                            <button
                                onClick={() => setConfirmDelete(false)}
                                className="p-1 rounded-md hover:bg-gray-100 text-gray-400 transition-colors"
                            >
                                <X className="w-3.5 h-3.5" />
                            </button>
                        </>
                    ) : (
                        <button
                            onClick={() => setConfirmDelete(true)}
                            title="삭제"
                            className="p-1 rounded-md hover:bg-red-50 text-gray-400 hover:text-red-500 transition-colors"
                        >
                            <Trash2 className="w-3.5 h-3.5" />
                        </button>
                    )}
                </div>
            </div>

            {/* Title */}
            <h3 className="text-sm font-semibold text-gray-800 leading-snug mb-3 group-hover:text-blue-600 transition-colors">
                {task.title}
            </h3>

            {/* Bottom row: assignee + move button */}
            <div className="flex items-center justify-between">
                {task.assignee ? (
                    <div className="flex items-center gap-1.5">
                        <div className="w-5 h-5 rounded-full bg-gradient-to-br from-blue-400 to-indigo-600 flex items-center justify-center text-white text-[9px] font-bold flex-shrink-0">
                            {task.assignee.charAt(0).toUpperCase()}
                        </div>
                        <span className="text-xs text-gray-600 font-medium">{task.assignee}</span>
                    </div>
                ) : (
                    <span className="text-[11px] text-gray-300 italic">담당자 없음</span>
                )}

                {onMoveToInProgress && (
                    <button
                        onClick={onMoveToInProgress}
                        title="In Progress로 이동"
                        className="text-[10px] font-semibold text-amber-600 bg-amber-50 hover:bg-amber-100 border border-amber-200 px-2 py-0.5 rounded-full transition-colors opacity-0 group-hover:opacity-100 flex items-center gap-1"
                    >
                        <Clock className="w-3 h-3" /> 진행 중
                    </button>
                )}
            </div>
        </div>
    );
}

/** CommitHistory 카드 (Done 열 전용, 읽기 전용) */
function CommitCard({ commit }: { commit: CommitHistory }) {
    const shortSha = commit.commitSha.slice(0, 7);
    const date = commit.commitDate
        ? new Date(commit.commitDate).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
        : '';

    return (
        <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-shadow hover:border-green-200 group">
            <div className="flex items-start justify-between mb-2">
                <span className="text-[11px] font-mono font-medium text-gray-400 bg-gray-100 px-2 py-0.5 rounded flex items-center gap-1">
                    <GitCommitHorizontal className="w-3 h-3" />
                    {shortSha}
                </span>
                <span className="text-[10px] text-gray-400">{date}</span>
            </div>
            <p className="text-sm font-semibold text-gray-800 leading-snug mb-3 line-clamp-2 group-hover:text-green-700 transition-colors">
                {commit.message}
            </p>
            <div className="flex items-center gap-1.5">
                <div className="w-5 h-5 rounded-full bg-gradient-to-br from-green-400 to-emerald-600 flex items-center justify-center text-white text-[9px] font-bold flex-shrink-0">
                    {commit.author.charAt(0).toUpperCase()}
                </div>
                <span className="text-xs text-gray-600 font-medium">{commit.author}</span>
            </div>
        </div>
    );
}

// ─── Drag & Drop Hook ─────────────────────────────────────────────────────────

function useDragDrop(
    tasks: BoardTask[],
    onDrop: (taskId: number, newStatus: 'TODO' | 'IN_PROGRESS') => Promise<void>,
) {
    const [draggingId, setDraggingId] = useState<number | null>(null);

    const handleDragStart = useCallback((taskId: number) => {
        setDraggingId(taskId);
    }, []);

    const handleDrop = useCallback(
        async (columnStatus: 'Todo' | 'In Progress') => {
            if (draggingId == null) return;
            const task = tasks.find((t) => t.id === draggingId);
            if (!task) return;
            const newStatus = toApiStatus(columnStatus) as 'TODO' | 'IN_PROGRESS';
            if (task.status !== newStatus) {
                await onDrop(draggingId, newStatus);
            }
            setDraggingId(null);
        },
        [draggingId, tasks, onDrop],
    );

    const handleDragEnd = useCallback(() => setDraggingId(null), []);

    return { draggingId, handleDragStart, handleDrop, handleDragEnd };
}

// ─── Main Component ───────────────────────────────────────────────────────────

export function ProcessFlowView() {
    const teamCode = getTeamCode();

    // ── Local state ──
    const [selectedReleaseId, setSelectedReleaseId] = useState<number>(DUMMY_RELEASES[2].id);

    // Custom tasks from backend
    const [tasks, setTasks] = useState<BoardTask[]>([]);
    const [tasksLoading, setTasksLoading] = useState(true);
    const [tasksError, setTasksError] = useState<string | null>(null);

    // Commit history (Done column)
    const [commits, setCommits] = useState<CommitHistory[]>([]);
    const [commitsLoading, setCommitsLoading] = useState(true);

    // UI state
    const [showAddForm, setShowAddForm] = useState<'Todo' | 'In Progress' | null>(null);
    const [editingTaskId, setEditingTaskId] = useState<number | null>(null);

    // Drop zone highlight
    const [dropTarget, setDropTarget] = useState<'Todo' | 'In Progress' | null>(null);

    // ── Data fetching ──
    const fetchTasks = useCallback(async () => {
        try {
            setTasksLoading(true);
            setTasksError(null);
            const data = await getTasks(teamCode);
            setTasks(data);
        } catch {
            setTasksError('태스크를 불러오지 못했습니다.');
        } finally {
            setTasksLoading(false);
        }
    }, [teamCode]);

    const fetchCommits = useCallback(async () => {
        try {
            setCommitsLoading(true);
            const data = await getCommits(teamCode);
            setCommits(data);
        } catch {
            // silently ignore – Done column is best-effort
        } finally {
            setCommitsLoading(false);
        }
    }, [teamCode]);

    useEffect(() => {
        fetchTasks();
        fetchCommits();
    }, [fetchTasks, fetchCommits]);

    // ── Task CRUD ──
    const handleAddTask = useCallback(
        async (form: TaskFormState, status: BoardTaskStatus) => {
            const created = await createTask(teamCode, {
                title: form.title,
                assignee: form.assignee || undefined,
                label: form.label || undefined,
                status,
            });
            setTasks((prev) => [created, ...prev]);
            setShowAddForm(null);
        },
        [teamCode],
    );

    const handleEditTask = useCallback(
        async (task: BoardTask, form: TaskFormState) => {
            const updated = await updateTask(task.id, {
                title: form.title,
                assignee: form.assignee || undefined,
                label: form.label || undefined,
            });
            setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
            setEditingTaskId(null);
        },
        [],
    );

    const handleDeleteTask = useCallback(async (taskId: number) => {
        await deleteTask(taskId);
        setTasks((prev) => prev.filter((t) => t.id !== taskId));
    }, []);

    const handleMoveTask = useCallback(
        async (taskId: number, newStatus: BoardTaskStatus) => {
            const updated = await updateTask(taskId, { status: newStatus });
            setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
        },
        [],
    );

    // ── Drag & Drop ──
    const { draggingId, handleDragStart, handleDrop, handleDragEnd } = useDragDrop(
        tasks,
        handleMoveTask,
    );

    // ── Derived state ──
    const todoTasks     = tasks.filter((t) => t.status === 'TODO');
    const inProgTasks   = tasks.filter((t) => t.status === 'IN_PROGRESS');

    // ── Render helpers ──
    const renderTodoColumn = () => (
        <div
            key="Todo"
            className={`flex-1 min-w-[300px] flex flex-col bg-gray-100/50 rounded-xl p-4 border transition-colors
                ${dropTarget === 'Todo' ? 'border-blue-400 bg-blue-50/40' : 'border-gray-200/60'}`}
            onDragOver={(e) => { e.preventDefault(); setDropTarget('Todo'); }}
            onDragLeave={() => setDropTarget(null)}
            onDrop={() => { setDropTarget(null); handleDrop('Todo'); }}
        >
            {/* Column Header */}
            <div className="flex items-center justify-between mb-4 px-2">
                <div className="flex items-center gap-2 font-semibold text-gray-700 text-sm">
                    {getStatusIcon('Todo')} Todo
                </div>
                <span className="bg-gray-200 text-gray-600 text-xs font-bold px-2 py-0.5 rounded-full">
                    {todoTasks.length}
                </span>
            </div>

            {/* Cards */}
            <div className="flex flex-col gap-3 overflow-y-auto flex-1">
                {tasksLoading ? (
                    <div className="flex items-center justify-center py-8 text-gray-400">
                        <Loader2 className="w-5 h-5 animate-spin mr-2" /> 불러오는 중...
                    </div>
                ) : tasksError ? (
                    <div className="text-center py-6 text-red-400 text-sm">{tasksError}</div>
                ) : (
                    todoTasks.map((task) =>
                        editingTaskId === task.id ? (
                            <EditTaskForm
                                key={task.id}
                                task={task}
                                onSave={(form) => handleEditTask(task, form)}
                                onCancel={() => setEditingTaskId(null)}
                            />
                        ) : (
                            <div
                                key={task.id}
                                draggable
                                onDragStart={() => handleDragStart(task.id)}
                                onDragEnd={handleDragEnd}
                            >
                                <TaskCard
                                    task={task}
                                    onEdit={() => setEditingTaskId(task.id)}
                                    onDelete={() => handleDeleteTask(task.id)}
                                    onMoveToInProgress={() => handleMoveTask(task.id, 'IN_PROGRESS')}
                                    isDragging={draggingId === task.id}
                                    dragHandleProps={{}}
                                />
                            </div>
                        )
                    )
                )}

                {!tasksLoading && !tasksError && todoTasks.length === 0 && showAddForm !== 'Todo' && (
                    <div className="text-center p-6 border-2 border-dashed border-gray-200 rounded-xl text-gray-400 text-sm">
                        태스크가 없습니다
                    </div>
                )}

                {/* Inline Add Form */}
                {showAddForm === 'Todo' && (
                    <AddTaskForm
                        onAdd={(form) => handleAddTask(form, 'TODO')}
                        onCancel={() => setShowAddForm(null)}
                    />
                )}
            </div>

            {/* Add Button */}
            {showAddForm !== 'Todo' && (
                <button
                    onClick={() => { setShowAddForm('Todo'); setEditingTaskId(null); }}
                    className="mt-3 w-full flex items-center justify-center gap-1.5 text-xs font-semibold text-gray-500 hover:text-blue-600 hover:bg-blue-50 border border-dashed border-gray-300 hover:border-blue-400 py-2 rounded-xl transition-all"
                >
                    <Plus className="w-3.5 h-3.5" /> 태스크 추가
                </button>
            )}
        </div>
    );

    const renderInProgressColumn = () => (
        <div
            key="In Progress"
            className={`flex-1 min-w-[300px] flex flex-col bg-gray-100/50 rounded-xl p-4 border transition-colors
                ${dropTarget === 'In Progress' ? 'border-amber-400 bg-amber-50/40' : 'border-gray-200/60'}`}
            onDragOver={(e) => { e.preventDefault(); setDropTarget('In Progress'); }}
            onDragLeave={() => setDropTarget(null)}
            onDrop={() => { setDropTarget(null); handleDrop('In Progress'); }}
        >
            {/* Column Header */}
            <div className="flex items-center justify-between mb-4 px-2">
                <div className="flex items-center gap-2 font-semibold text-gray-700 text-sm">
                    {getStatusIcon('In Progress')} In Progress
                </div>
                <span className="bg-amber-100 text-amber-700 text-xs font-bold px-2 py-0.5 rounded-full">
                    {inProgTasks.length}
                </span>
            </div>

            {/* Cards */}
            <div className="flex flex-col gap-3 overflow-y-auto flex-1">
                {tasksLoading ? (
                    <div className="flex items-center justify-center py-8 text-gray-400">
                        <Loader2 className="w-5 h-5 animate-spin mr-2" /> 불러오는 중...
                    </div>
                ) : (
                    inProgTasks.map((task) =>
                        editingTaskId === task.id ? (
                            <EditTaskForm
                                key={task.id}
                                task={task}
                                onSave={(form) => handleEditTask(task, form)}
                                onCancel={() => setEditingTaskId(null)}
                            />
                        ) : (
                            <div
                                key={task.id}
                                draggable
                                onDragStart={() => handleDragStart(task.id)}
                                onDragEnd={handleDragEnd}
                            >
                                <TaskCard
                                    task={task}
                                    onEdit={() => setEditingTaskId(task.id)}
                                    onDelete={() => handleDeleteTask(task.id)}
                                    isDragging={draggingId === task.id}
                                    dragHandleProps={{}}
                                />
                            </div>
                        )
                    )
                )}

                {!tasksLoading && inProgTasks.length === 0 && showAddForm !== 'In Progress' && (
                    <div className="text-center p-6 border-2 border-dashed border-gray-200 rounded-xl text-gray-400 text-sm">
                        진행 중인 태스크 없음
                    </div>
                )}

                {showAddForm === 'In Progress' && (
                    <AddTaskForm
                        onAdd={(form) => handleAddTask(form, 'IN_PROGRESS')}
                        onCancel={() => setShowAddForm(null)}
                    />
                )}
            </div>

            {/* Add Button */}
            {showAddForm !== 'In Progress' && (
                <button
                    onClick={() => { setShowAddForm('In Progress'); setEditingTaskId(null); }}
                    className="mt-3 w-full flex items-center justify-center gap-1.5 text-xs font-semibold text-gray-500 hover:text-amber-600 hover:bg-amber-50 border border-dashed border-gray-300 hover:border-amber-400 py-2 rounded-xl transition-all"
                >
                    <Plus className="w-3.5 h-3.5" /> 태스크 추가
                </button>
            )}
        </div>
    );

    const renderDoneColumn = () => (
        <div
            key="Done"
            className="flex-1 min-w-[300px] flex flex-col bg-gray-100/50 rounded-xl p-4 border border-gray-200/60"
        >
            {/* Column Header */}
            <div className="flex items-center justify-between mb-4 px-2">
                <div className="flex items-center gap-2 font-semibold text-gray-700 text-sm">
                    {getStatusIcon('Done')} Done
                    <span className="text-[10px] font-normal text-gray-400 ml-1">· GitHub Commits</span>
                </div>
                <span className="bg-green-100 text-green-700 text-xs font-bold px-2 py-0.5 rounded-full">
                    {commits.length}
                </span>
            </div>

            {/* Commit Cards */}
            <div className="flex flex-col gap-3 overflow-y-auto flex-1">
                {commitsLoading ? (
                    <div className="flex items-center justify-center py-8 text-gray-400">
                        <Loader2 className="w-5 h-5 animate-spin mr-2" /> 불러오는 중...
                    </div>
                ) : commits.length === 0 ? (
                    <div className="text-center p-6 border-2 border-dashed border-gray-200 rounded-xl text-gray-400 text-sm">
                        커밋 내역이 없습니다
                    </div>
                ) : (
                    commits.slice(0, 20).map((commit) => (
                        <CommitCard key={commit.id} commit={commit} />
                    ))
                )}
            </div>
        </div>
    );

    // ── Main Render ──────────────────────────────────────────────────────────

    return (
        <div className="flex flex-col h-full bg-[#FAFAFA]">

            {/* ── Release Timeline ── */}
            <div className="bg-white border-b border-gray-200 p-8 flex-shrink-0">
                <h2 className="text-xl font-bold text-gray-900 mb-6 flex items-center gap-2">
                    <GitBranch className="w-6 h-6" /> Release Timeline
                </h2>

                <div className="flex items-start overflow-x-auto pb-4 hide-scrollbar">
                    {DUMMY_RELEASES.map((release, index) => {
                        const isSelected = selectedReleaseId === release.id;
                        const progressPct = Math.round((release.completedTasks / release.totalTasks) * 100) || 0;

                        return (
                            <React.Fragment key={release.id}>
                                <div
                                    className={`flex flex-col items-center cursor-pointer transition-all ${isSelected ? 'opacity-100 scale-105' : 'opacity-60 hover:opacity-100'}`}
                                    onClick={() => setSelectedReleaseId(release.id)}
                                >
                                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border-2 mb-3 shadow-sm
                                        ${release.type === 'hotfix'
                                            ? (isSelected ? 'border-purple-600 bg-purple-50 text-purple-700' : 'border-gray-200 bg-white')
                                            : (isSelected ? 'border-blue-600 bg-blue-50 text-blue-700'   : 'border-gray-200 bg-white')
                                        }`}
                                    >
                                        <span className="font-mono text-sm font-bold">
                                            {release.type === 'hotfix' ? 'H' : 'R'}
                                        </span>
                                    </div>

                                    <div className="text-center w-28">
                                        <p className={`text-sm font-bold truncate ${isSelected ? 'text-gray-900' : 'text-gray-600'}`}>
                                            {release.name}
                                        </p>
                                        <p className="text-xs text-gray-400 mb-2">{release.date}</p>

                                        <div className="w-full bg-gray-100 rounded-full h-1.5 mb-1 overflow-hidden">
                                            <div
                                                className={`h-1.5 rounded-full transition-all ${release.type === 'hotfix' ? 'bg-purple-500' : 'bg-blue-500'}`}
                                                style={{ width: `${progressPct}%` }}
                                            />
                                        </div>
                                        <p className="text-[10px] font-medium text-gray-500">
                                            {release.completedTasks}/{release.totalTasks} Done
                                        </p>
                                    </div>
                                </div>

                                {index < DUMMY_RELEASES.length - 1 && (
                                    <div className="w-16 h-px bg-gray-300 border-t-2 border-dashed border-gray-300 mt-6 mx-2" />
                                )}
                            </React.Fragment>
                        );
                    })}
                </div>
            </div>

            {/* ── Kanban Area ── */}
            <div className="flex-1 overflow-hidden flex flex-col p-8">
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
                        <LayoutGrid className="w-6 h-6 text-gray-400" /> Task Board
                    </h2>
                    <div className="text-xs text-gray-400 flex items-center gap-1.5 bg-white border border-gray-200 px-3 py-1.5 rounded-lg">
                        <GripVertical className="w-3.5 h-3.5" />
                        카드를 드래그하여 상태를 변경하세요
                    </div>
                </div>

                {/* Three Columns */}
                <div className="flex-1 flex gap-6 overflow-x-auto pb-4">
                    {renderTodoColumn()}
                    {renderInProgressColumn()}
                    {renderDoneColumn()}
                </div>
            </div>

        </div>
    );
}