# YadaFramework Overview

## Core Principles
- Focus on productivity through proven technologies
- Emphasis on simplicity (KISS principle)
- Use of established, well-documented technologies
- Preference for simple architectures that solve practical problems

## Technology Stack
- Java (17+)
- Tomcat web server
- Spring Web MVC
- MySQL Community Server
- Hibernate ORM
- Thymeleaf templating
- jQuery and JavaScript
- HTML, CSS, SASS
- Development tools:
  * Eclipse IDE
  * Gradle build system
  * Git version control
  * Asciidoctor for documentation

## Core Modules

### YadaWeb (yadaweb)
- Base web application framework
- Spring-based architecture
- Core web functionality and utilities
- Tomcat server integration (YadaTomcatServer)
- Configuration management system

### YadaWebSecurity (yadawebsecurity)
- Authentication and authorization
- User management system
- Registration and login flows
- Social media integration
- Auto-login token management
- Security configuration

### YadaWebCMS (yadawebcms)
- Content Management System
- Article management
- Product catalog system
- File attachment handling
- Content versioning

### YadaWebCommerce (yadawebcommerce)
- E-commerce functionality
- Order management
- Shopping cart system
- Product configurations
- Pricing management

### YadaTools (yadatools)
- Build and development tools
- Database schema generation
- Project initialization utilities
- Deployment helpers

## Framework Features

### Configuration System
- Hierarchical configuration with XML
- Environment-specific overrides (dev, test, prod)
- External property file support
- Hot-reloading capabilities
- XPath-based configuration engine

### Database Integration
- JPA/Hibernate ORM support
- Flyway database migrations
- Schema generation tools
- Connection pooling
- Transaction management

### Email System
- Template-based email system
- Multi-language support
- HTML email templates
- Common email flows:
  - Registration
  - Password recovery
  - Email verification
  - System notifications

### Security Features
- Spring Security integration
- User authentication and authorization
- Role-based access control
- Configuration options:
  * Session timeout management
  * Password policies and encoding
  * Failed login attempt handling
  * Auto-login token management
- User profile customization
- Root user setup for initial admin access
- Social login integration
- CSRF protection (optional)

### Frontend Integration
- SASS/SCSS preprocessing
- JavaScript minification
- Resource optimization
- Asset management
- Thymeleaf template engine
- Form handling features:
  * Standard Spring MVC form support
  * Form backing beans
  * Entity-backed forms
  * AJAX form submission
  * Form grouping for multi-form submissions
  * Validation in modals
  * File uploads and image galleries
  * Enhanced input field components

## Project Integration

### Gradle Configuration
```gradle
dependencies {
    implementation 'net.yadaframework:yadaweb:0.7.5'
    implementation 'net.yadaframework:yadawebcms:0.7.5'
    implementation 'net.yadaframework:yadawebsecurity:0.7.5'
    implementation 'net.yadaframework:yadawebcommerce:0.7.5'
}
```

### Local Development Setup
1. Configure local properties:
   ```properties
   yadaSourceRepoPath = /path/to/yadaframework
   yadaProjects = YadaWeb,YadaWebCMS,YadaWebSecurity,YadaWebCommerce
   ```

2. Directory structure:
   ```
   src/
   ├── main/
   │   ├── java/
   │   │   └── your/package/
   │   └── resources/
   │       ├── configuration.xml
   │       ├── conf.webapp.dev.xml
   │       ├── conf.webapp.prod.xml
   │       └── META-INF/
   │           └── persistence.xml
   ```

3. Required configurations:
   - configuration.xml: Main framework configuration
   - Environment-specific webapp configs
   - persistence.xml for database setup

### Running Applications
1. Embedded Tomcat:
   - Use YadaTomcatServer for development
   - Supports multiple ports and configurations
   - Hot-reloading capabilities

2. WAR Deployment:
   - Traditional WAR file deployment
   - Environment-specific builds
   - Automated deployment scripts

## Best Practices

### Configuration Management
1. Use hierarchical configuration:
   - Base configuration in configuration.xml
   - Environment overrides in conf.webapp.{env}.xml
   - Local developer overrides in conf.webapp.localdev.xml

2. Security properties:
   - Keep sensitive data in external files
   - Use environment-specific security.properties
   - Configure proper file permissions

### Development Workflow
1. Local Development:
   - Use embedded Tomcat server
   - Enable hot-reloading
   - Configure local database

2. Testing:
   - Deploy to test environment
   - Use separate database
   - Enable detailed logging

3. Production:
   - Optimize configurations
   - Disable development features
   - Configure proper security

### Common Pitfalls
1. Persistence unit conflicts when running in IDE
2. Missing security.properties file
3. Incorrect database credentials
4. Misconfigured email templates
5. Wrong environment configuration loaded
6. Form validation errors not properly handled
7. AJAX response handling misconfiguration
8. Form group naming conflicts

## Version Compatibility

### Requirements
- Java 17+
- Spring Framework 5+
- Hibernate 5+
- MySQL 8+ (recommended)
- Tomcat 8.5+

### Module Versions
Always use matching versions across Yada modules to ensure compatibility:
```gradle
def yadaVersion = '0.7.5'
implementation "net.yadaframework:yadaweb:${yadaVersion}"
implementation "net.yadaframework:yadawebcms:${yadaVersion}"
implementation "net.yadaframework:yadawebsecurity:${yadaVersion}"
```