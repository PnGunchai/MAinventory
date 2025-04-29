// Define role-based permissions
const ROLE_PERMISSIONS = {
    ADMIN: [
        'view_all',
        'edit_all',
        'delete_all',
        'canAddStock',
        'canRemoveStock',
        'canCreateOrder',
        'canEditOrder',
        'canDeleteOrder'
    ],
    USER: ['view_all', 'edit_all'],
    SALES: ['view_stock', 'view_orders']
};

// Check if a role has a specific permission
export const checkPermission = (role, permission) => {
    if (!role || !permission) return false;
    return ROLE_PERMISSIONS[role]?.includes(permission) || false;
};

// Get all permissions for a role
export const getRolePermissions = (role) => {
    return ROLE_PERMISSIONS[role] || [];
};

// Check if a path is allowed for a role
export const isPathAllowed = (role, path) => {
    if (!role || !path) return false;
    
    // Define allowed paths for each role
    const ALLOWED_PATHS = {
        ADMIN: ['/home', '/catalog', '/stock', '/orders', '/records', '/dashboard', '/logs'],
        USER: ['/home', '/catalog', '/stock', '/orders', '/records', '/dashboard', '/logs'],
        SALES: ['/home', '/stock', '/orders', '/dashboard']
    };

    // Always allow these paths
    const PUBLIC_PATHS = ['/', '/login'];
    
    if (PUBLIC_PATHS.includes(path)) return true;
    return ALLOWED_PATHS[role]?.some(allowedPath => path.startsWith(allowedPath)) || false;
}; 