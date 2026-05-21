import re

with open("src/main/java/com/example/bff/controller/ProxyController.java", "r") as f:
    content = f.read()

# Replace @PostMapping("/AuthForward/**")
content = content.replace(
    '@PostMapping("/AuthForward/**")',
    '@RequestMapping(value = "/AuthForward/**", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})'
)
# Add HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod()); inside forwardPostRequest
content = re.sub(
    r'(public ResponseEntity<\?> forwardPostRequest\([\s\S]*?\{)',
    r'\1\n        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());',
    content
)

# Replace @PostMapping("/Main/**")
content = content.replace(
    '@PostMapping("/Main/**")',
    '@RequestMapping(value = "/Main/**", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})'
)
# Add HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod()); inside MainForwardPostRequest
content = re.sub(
    r'(public ResponseEntity<\?> MainForwardPostRequest\([\s\S]*?\{)',
    r'\1\n        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());',
    content
)

# Replace all instances of HttpMethod.POST with httpMethod inside ProxyController
# But wait, we need to make sure we don't break anything. Let's just do it string replacement where it matches.
content = content.replace('HttpMethod.POST', 'httpMethod')

# Also we need to make sure we import RequestMethod if it is not imported
if 'import org.springframework.web.bind.annotation.RequestMethod;' not in content:
    content = content.replace('import org.springframework.web.bind.annotation.RestController;', 'import org.springframework.web.bind.annotation.RestController;\nimport org.springframework.web.bind.annotation.RequestMethod;')

with open("src/main/java/com/example/bff/controller/ProxyController.java", "w") as f:
    f.write(content)

