import os
from dotenv import load_dotenv
from pathlib import Path
from supabase import create_client, Client
from github_analyzer import analyze_single_repository, fetch_recent_commits

def main():
    print("🚀 Soft On-boarding Agent [데이터 추출 검증 파이프라인] 시작\n")
    
    env_path = Path(__file__).parent / ".env"
    load_dotenv(dotenv_path=env_path)
    
    USERNAME = os.getenv("GITHUB_USERNAME")
    GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
    SUPABASE_URL = os.getenv("SUPABASE_URL") 
    SUPABASE_KEY = os.getenv("SUPABASE_KEY") 
    TARGET_REPO = os.getenv("TARGET_REPO", "TOP250movie_douban") 

    if not USERNAME or not GITHUB_TOKEN:
        print("❌ 오류: .env 파일 설정을 확인해주세요.")
        return

    supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

    print(f"🔍 [로직 A] '{TARGET_REPO}' 구조 추출 중...")
    categorized_files = analyze_single_repository(USERNAME, GITHUB_TOKEN, TARGET_REPO)
    print("✅ [로직 A 완료] 4개 카테고리 추출 완료!\n")

    print(f"🔍 [로직 B] '{TARGET_REPO}' 최신 커밋 추출 중...")
    recent_commits = fetch_recent_commits(USERNAME, GITHUB_TOKEN, TARGET_REPO, limit=100)
    print("✅ [로직 B 완료] 100개의 커밋 히스토리 로드 완료!\n") # limit을 꽉 채웠으므로 100개로 고정 출력
    
    print("☁️ 수파베이스 스토리지에 데이터 매핑 파일 5개 업로드를 시작합니다...\n")

    # [1] 로직 A (4개 파일)
    categories = ["Interface", "Functional", "Data", "Process"]
    for cat in categories:
        files_list = categorized_files.get(cat, [])
        file_name = f"{TARGET_REPO}_LogicA_{cat}_DB_Mock.md"
        
        content = f"# 📂 {TARGET_REPO} - {cat} View 컴포넌트 목록 (DB 저장용 데이터)\n\n"
        if not files_list:
            content += "- 매핑된 파일이 없습니다.\n"
        else:
            for f_path in files_list:
                content += f"- `{f_path}`\n"
                
        # 🔥 필살기 적용: utf-8 대신 utf-8-sig 를 사용해 파일에 UTF-8 도장을 강제로 찍습니다.
        with open(file_name, "w", encoding="utf-8-sig") as f:
            f.write(content)
            
        try:
            supabase.storage.from_("onboarding-guides").upload(
                file=open(file_name, "rb"), 
                path=file_name, 
                file_options={"content-type": "text/markdown; charset=utf-8", "upsert": "true"}
            )
            public_url = supabase.storage.from_("onboarding-guides").get_public_url(file_name)
            print(f"  ✅ [로직 A] {file_name} 업로드 완료!")
            print(f"     🔗 공유 링크: {public_url}\n")
        except Exception as e:
            print(f"  ❌ [로직 A] {file_name} 업로드 실패: {e}\n")
        finally:
            if os.path.exists(file_name):
                os.remove(file_name)

    # [2] 로직 B (1개 파일)
    commit_file_name = f"{TARGET_REPO}_LogicB_CommitHistory_DB_Mock.md"
    commit_content = f"# 🕒 {TARGET_REPO} - 최신 커밋 히스토리 100개 (DB 저장용 데이터)\n\n"
    
    if not recent_commits:
        commit_content += "- 커밋 내역이 없습니다.\n"
    else:
        for commit_data in recent_commits:
            sha = commit_data.get('sha', '')[:7] 
            info = commit_data.get('commit', {})
            msg = info.get('message', '').split('\n')[0] 
            date = info.get('author', {}).get('date', '')[:10] 
            author = info.get('author', {}).get('name', '')
            commit_content += f"- **[{date}]** `{sha}` : {msg} (by {author})\n"

    # 🔥 여기도 필살기 적용!
    with open(commit_file_name, "w", encoding="utf-8-sig") as f:
        f.write(commit_content)
        
    try:
        supabase.storage.from_("onboarding-guides").upload(
            file=open(commit_file_name, "rb"), 
            path=commit_file_name, 
            file_options={"content-type": "text/markdown; charset=utf-8", "upsert": "true"}
        )
        public_url = supabase.storage.from_("onboarding-guides").get_public_url(commit_file_name)
        print(f"  ✅ [로직 B] {commit_file_name} 업로드 완료!")
        print(f"     🔗 공유 링크: {public_url}\n")
    except Exception as e:
        print(f"  ❌ [로직 B] {commit_file_name} 업로드 실패: {e}\n")
    finally:
        if os.path.exists(commit_file_name):
            os.remove(commit_file_name)

    print("🎉 총 5개의 데이터 파일 업로드 및 링크 추출이 완료되었습니다!")

if __name__ == "__main__":
    main()