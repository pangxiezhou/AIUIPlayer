@echo off

chcp 65001

git branch | find "* master" > NUL & IF ERRORLEVEL 1 (
	ECHO 警告：不处于master分支，此命令仅支持在master分支运行
) ELSE (
	setlocal
	:PROMPT
	SET /P AREYOUSURE=确认同步代码到github吗 (Y/[N])
	IF /I "%AREYOUSURE%" NEQ "Y" GOTO END
	
	echo 1. 同步代码到svn
	git svn dcommit
	
	if ERRORLEVEL 1 (
		echo 同步代码到SVN出错，检查后重试
		GOTO END
	) ELSE (
		echo 同步代码到SVN成功
	)
	
	echo 2. 删除重建github分支
	git branch | find "* github" > NUL & IF ERRORLEVEL 0 (
		git branch -D github
	)
	git checkout -b github
	
	echo 3. 删除敏感文件
	git filter-branch --force --index-filter "git rm -r --cached --ignore-unmatch doc" --prune-empty --tag-name-filter cat
	git filter-branch --force --index-filter "git rm -r --cached --ignore-unmatch player_init/src/main/cpp" --prune-empty --tag-name-filter cat 
	git filter-branch --force --index-filter "git rm -r --cached --ignore-unmatch player_init/CMakeLists.txt" --prune-empty --tag-name-filter cat 

	echo 4. 添加github remote
	git remote | find "github_origin" > NUL & IF ERRORLEVEL 0 (
		git remote remove github_origin
	)
	git remote add github_origin git@github.com:pangxiezhou/AIUIPlayer.git
	
	echo 5. 同步到github
	git push -f -u --tags github_origin github:master
	
	echo 6. 切换回master分支
	git checkout master
)

:END
endlocal

pause