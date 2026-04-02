document.addEventListener('DOMContentLoaded', function() {
    const roleButtons = document.querySelectorAll('.role-btn');
    const loginForm = document.getElementById('loginForm');
    const errorMessage = document.getElementById('errorMessage');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const hintText = document.getElementById('hintText');
    
    let selectedRole = 'STUDENT';
    
    roleButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            roleButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            selectedRole = this.dataset.role;
            updateCredentialHint(selectedRole);
            autoFillCredentials(selectedRole);
        });
    });
    
    function updateCredentialHint(role) {
        if (role === 'HOD') {
            hintText.innerHTML = '👤 HOD: hod.cse@example.com / 1234';
        } else if (role === 'FACULTY') {
            hintText.innerHTML = '👨‍🏫 Faculty: amit.sharma@example.com / 1234';
        } else {
            hintText.innerHTML = '👨‍🎓 Student: student1@example.com / 1234';
        }
    }
    
    function autoFillCredentials(role) {
        if (role === 'HOD') {
            emailInput.value = 'hod.cse@example.com';
            passwordInput.value = '1234';
        } else if (role === 'FACULTY') {
            emailInput.value = 'amit.sharma@example.com';
            passwordInput.value = '1234';
        } else {
            emailInput.value = 'student1@example.com';
            passwordInput.value = '1234';
        }
    }
    
    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const email = emailInput.value.trim();
        const password = passwordInput.value.trim();
        
        if (!email || !password) {
            errorMessage.textContent = 'Please enter both email and password';
            return;
        }
        
        const submitBtn = document.querySelector('.login-btn');
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Logging in...';
        submitBtn.disabled = true;
        errorMessage.textContent = '';
        
        try {
            console.log('Logging in with:', email, password);
            
            const response = await fetch('http://localhost:8080/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });
            
            const data = await response.json();
            console.log('Login response:', data);
            
            if (data.success) {
                if (data.role !== selectedRole) {
                    errorMessage.textContent = `Invalid role. You selected ${selectedRole} but logged in as ${data.role}`;
                    submitBtn.innerHTML = originalText;
                    submitBtn.disabled = false;
                    return;
                }
                
                sessionStorage.setItem('user', JSON.stringify(data));
                
                errorMessage.style.color = 'green';
                errorMessage.textContent = 'Login successful! Redirecting...';
                
                setTimeout(() => {
                    switch(data.role) {
                        case 'STUDENT':
                            window.location.href = 'student-dashboard.html';
                            break;
                        case 'FACULTY':
                            window.location.href = 'faculty-dashboard.html';
                            break;
                        case 'HOD':
                            window.location.href = 'hod-dashboard.html';
                            break;
                        default:
                            window.location.href = 'login.html';
                    }
                }, 1000);
            } else {
                errorMessage.textContent = data.message || 'Invalid email or password';
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
            }
        } catch (error) {
            console.error('Login error:', error);
            errorMessage.textContent = 'Connection error. Make sure server is running on port 8080';
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        }
    });
    
    // Test connection function
    window.testConnection = async function() {
        try {
            const response = await fetch('http://localhost:8080/api/test');
            const data = await response.json();
            alert('✅ Server is running!\n\n' + JSON.stringify(data, null, 2));
        } catch (error) {
            alert('❌ Cannot connect to server. Please check if backend is running on port 8080');
        }
    };
    
    // Auto-fill student credentials by default
    autoFillCredentials('STUDENT');
    updateCredentialHint('STUDENT');
});