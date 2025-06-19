import { NextResponse } from 'next/server';
import { isPathAllowed } from './utils/permissions';

// Helper function to get user data from cookies
function getUserFromCookies(request) {
  const userCookie = request.cookies.get('user');
  const tokenCookie = request.cookies.get('token');
  
  // Check for undefined or invalid cookie values
  if (!userCookie?.value || userCookie.value === 'undefined' || 
      !tokenCookie?.value || tokenCookie.value === 'undefined') {
    // Clear invalid cookies
    const response = NextResponse.next();
    response.cookies.delete('user');
    response.cookies.delete('token');
    return null;
  }
  
  try {
    const userData = JSON.parse(userCookie.value);
    // Validate user data structure
    if (!userData || typeof userData !== 'object' || !userData.access) {
      console.error('Invalid user data structure');
      return null;
    }
    return userData;
  } catch (error) {
    console.error('Error parsing user data:', error);
    // Clear invalid cookies
    const response = NextResponse.next();
    response.cookies.delete('user');
    response.cookies.delete('token');
    return null;
  }
}

export function middleware(request) {
  // Get the path from the URL
  const path = request.nextUrl.pathname;
  
  // Define public paths that don't require authentication
  const publicPaths = ['/login', '/register', '/', '/api/auth/login'];
  if (publicPaths.includes(path)) {
    // If user is already logged in and tries to access login/register, redirect to home
    const token = request.cookies.get('token');
    if (token?.value && token.value !== 'undefined' && (path === '/login' || path === '/register')) {
      return NextResponse.redirect(new URL('/dashboard', request.url));
    }
    return NextResponse.next();
  }

  // Get user data from cookies
  const user = getUserFromCookies(request);
  const token = request.cookies.get('token');

  // Redirect to login if not authenticated
  if (!token?.value || token.value === 'undefined' || !user) {
    const loginUrl = new URL('/login', request.url);
    // Add the original path as a redirect parameter
    loginUrl.searchParams.set('redirect', path);
    return NextResponse.redirect(loginUrl);
  }

  // Check if path is allowed for user's role
  if (!isPathAllowed(user.access, path)) {
    // Redirect to dashboard if authenticated but not authorized
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  return NextResponse.next();
}

// Configure which paths middleware will run on
export const config = {
  matcher: [
    // Add paths that should be protected or checked
    '/((?!_next/static|favicon.ico|api/auth/login).*)',
  ]
}; 