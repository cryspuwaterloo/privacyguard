
## ToDo List

- ~~Turn on/off VPN~~ 
	- ~~service side~~
	- ~~client side~~
		- ~~handle user forbidden permission~~ 
- DB: delete old leaks (maybe configurable?)
- DB: need to make DBhandler thread safe (since multiple filter threads)
- Test Async running
- Beta release
	- description
	- signed APK
- setting/about page for async
- Use string resource instead of constant value
- Improve logging practice
- Social Media integration (be able to share leakage statistics)
- ~~OnResume/OnStop as well as testing app life cycle/corner cases (force stop, etc.)~~
- Stress testing (ensure VPN does not cause performance issues
- User studies to gain feedback/Tweaks to front facing UI (increased height for table rows, icons, etc.)
- Rating system which associates each app with a five star rating based on leakage history/ show star rating below app icons on phone
- Allow an option for the user to share statistics with us so overall stats on apps can be aggregated
- Potentially display leakage stats as they become more complex in graphical form
- Plugins should be configurable from UI, not hardcoded
- Support TLS SNI and certificate pinning
- Don't fail if user refuses to trust fake CA certificate; in general, let user disable TLS interception, probably on a per-app basis
- Report IP destination address of leaks
- Remove certificate for fake root CA upon app uninstallation
- List of keywords to be filtered for should be directly editable in app
- Add feature to capture all traffic in pcap format
