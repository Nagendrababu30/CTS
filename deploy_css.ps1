$base = 'C:\Users\Jeevan bn\zk\.metadata\.plugins\org.eclipse.wst.server.core\tmp2\wtpwebapps\CTS'
Copy-Item 'src\main\webapp\css\common\loader.css' -Destination "$base\css\common\loader.css" -Force
Copy-Item 'src\main\webapp\css\common\common.css' -Destination "$base\css\common\common.css" -Force
Copy-Item 'src\main\webapp\css\common\login.css' -Destination "$base\css\common\login.css" -Force
Copy-Item 'src\main\webapp\css\inward\checker\batch-details.css' -Destination "$base\css\inward\checker\batch-details.css" -Force
Copy-Item 'src\main\webapp\zul\inward-checker\batch-details.zul' -Destination "$base\zul\inward-checker\batch-details.zul" -Force
Copy-Item 'src\main\webapp\zul\common\layout.zul' -Destination "$base\zul\common\layout.zul" -Force
(Get-Item "$base\WEB-INF\web.xml").LastWriteTime = Get-Date
Write-Output "Successfully deployed all files to Tomcat"
