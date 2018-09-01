@echo off

chcp 65001

git branch | find "* master" > NUL & IF ERRORLEVEL 0 (
    ECHO 处于master分支
	
	setlocal
	:PROMPT
	SET /P AREYOUSURE=确认同步代码到github吗 (Y/[N])
	IF /I "%AREYOUSURE%" NEQ "Y" GOTO END
	
	echo 同步代码到svn
	git svn dcommit
	
	if not ERRORLEVEL 0 (
		echo 同步代码到SVN出错，请自行检查
		GOTO END
	)
	
	echo 切换到clean分支
	git checkout clean
	git rebase master
	
	echo 删除重建github分支
	git branch -D github
	git checkout -b github
	
	echo 删除敏感文件
	git filter-branch --force --index-filter "git rm -r --cached --ignore-unmatch doc" --prune-empty --tag-name-filter cat -- --all
	git filter-branch --force --index-filter "git rm -r --cached --ignore-unmatch upload_github.bat" --prune-empty --tag-name-filter cat -- --all
	git filter-branch --force --index-filter "git rm -r --cached --ignore-unmatch player_init/src/main/cpp" --prune-empty --tag-name-filter cat -- --all
	git filter-branch --force --index-filter "git rm -r --cached --ignore-unmatch player_init/CMakeLists.txt" --prune-empty --tag-name-filter cat -- --all

	echo 添加github remote
	git remote | find "github_origin" > NUL & IF ERRORLEVEL 0 (
		git remote remove github_origin
	)
	git remote add github_origin git@github.com:pangxiezhou/AIUIPlayer.git
	
	echo 同步到github
	git push -f -u --tags github_origin github:master
	
) ELSE (
    ECHO 不处于master分支，此命令仅支持在master分支运行
)

:END
endlocal

pause