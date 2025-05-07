import React from 'react';

const Button = ({ 
  children, 
  onClick, 
  className = '', 
  type = 'button',
  disabled = false,
  ...props 
}) => {
  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
    }
  };

  return (
    <button
      type={type}
      onClick={onClick}
      onKeyDown={handleKeyDown}
      className={className}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );
};

export default Button; 