package net.osdn.jpki.pdf_signer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;

public class LogFilter extends LogFactory {

	public enum Level {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL,
		OFF
	};
	
	private static Map<String, Level> levels = new HashMap<String, Level>();
	
	public static void setLevel(Class<?> clazz, Level level) {
		levels.put(clazz.getName(), level);
	}
	
	public static void setLevel(String name, Level level) {
		levels.put(name, level);
	}
	
	private static Level getLevel(String name) {
		for(;;) {
			Level level = levels.get(name);
			if(level != null) {
				return level;
			}
			int i = name.lastIndexOf('.');
			if(i == -1) {
				return null;
			}
			name = name.substring(0, i);
		}
	}
	

    private LogFactory impl;
	
	public LogFilter() {
		impl = new LogFactoryImpl();
	}
	
	@Override
	public Object getAttribute(String name) {
		return impl.getAttribute(name);
	}

	@Override
	public String[] getAttributeNames() {
		return impl.getAttributeNames();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Log getInstance(Class clazz) throws LogConfigurationException {
		Log log = impl.getInstance(clazz);
		if(log != null) {
			return new Wrapper(log, getLevel(clazz.getName()));
		}
		return null;
	}

	@Override
	public Log getInstance(String name) throws LogConfigurationException {
		Log log = impl.getInstance(name);
		if(log != null) {
			return new Wrapper(log, getLevel(name));
		}
		return null;
	}

	@Override
	public void release() {
		impl.release();
	}

	@Override
	public void removeAttribute(String name) {
		impl.removeAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		impl.setAttribute(name, value);
	}

	public static class Wrapper implements Log {
		
		private Log impl;
		private boolean isTraceEnabled;
		private boolean isDebugEnabled;
		private boolean isInfoEnabled;
		private boolean isWarnEnabled;
		private boolean isErrorEnabled;
		private boolean isFatalEnabled;

		@SuppressWarnings("fallthrough")
		public Wrapper(Log impl, Level level) {
			this.impl = impl;

			if(level == null) {
				level = Level.TRACE;
			}
			switch(level) {
			case TRACE: isTraceEnabled = true;
			case DEBUG: isDebugEnabled = true;
			case INFO:  isInfoEnabled = true;
			case WARN:  isWarnEnabled = true;
			case ERROR: isErrorEnabled = true;
			case FATAL: isFatalEnabled = true;
			case OFF:
			}
		}
		
		public Wrapper setTraceEnabled(boolean b) {
			isTraceEnabled = b;
			return this;
		}

		public Wrapper setDebugEnabled(boolean b) {
			isDebugEnabled = b;
			return this;
		}

		public Wrapper setInfoEnabled(boolean b) {
			isInfoEnabled = b;
			return this;
		}
		
		public Wrapper setWarnEnabled(boolean b) {
			isWarnEnabled = b;
			return this;
		}

		public Wrapper setErrorEnabled(boolean b) {
			isErrorEnabled = b;
			return this;
		}

		public Wrapper setFatalEnabled(boolean b) {
			isFatalEnabled = b;
			return this;
		}
		
		@Override
		public boolean isTraceEnabled() {
			return isTraceEnabled && impl.isTraceEnabled();
		}

		@Override
		public boolean isDebugEnabled() {
			return isDebugEnabled && impl.isDebugEnabled();
		}

		@Override
		public boolean isInfoEnabled() {
			return isInfoEnabled && impl.isInfoEnabled();
		}
		
		@Override
		public boolean isWarnEnabled() {
			return isWarnEnabled && impl.isWarnEnabled();
		}

		@Override
		public boolean isErrorEnabled() {
			return isErrorEnabled && impl.isErrorEnabled();
		}

		@Override
		public boolean isFatalEnabled() {
			return isFatalEnabled && impl.isFatalEnabled();
		}

		@Override
		public void trace(Object message) {
			if(isTraceEnabled) {
				impl.trace(message);
			}
		}

		@Override
		public void trace(Object message, Throwable t) {
			if(isTraceEnabled) {
				impl.trace(message, t);
			}
		}

		@Override
		public void debug(Object message) {
			if(isDebugEnabled) {
				impl.debug(message);
			}
		}

		@Override
		public void debug(Object message, Throwable t) {
			if(isDebugEnabled) {
				impl.debug(message, t);
			}
		}

		@Override
		public void info(Object message) {
			if(isInfoEnabled) {
				impl.info(message);
			}
		}

		@Override
		public void info(Object message, Throwable t) {
			if(isInfoEnabled) {
				impl.info(message, t);
			}
		}

		@Override
		public void warn(Object message) {
			if(isWarnEnabled) {
				impl.warn(message);
			}
		}

		@Override
		public void warn(Object message, Throwable t) {
			if(isWarnEnabled) {
				impl.warn(message, t);
			}
		}
		
		@Override
		public void error(Object message) {
			if(isErrorEnabled) {
				impl.error(message);
			}
		}

		@Override
		public void error(Object message, Throwable t) {
			if(isErrorEnabled) {
				impl.error(message, t);
			}
		}

		@Override
		public void fatal(Object message) {
			if(isFatalEnabled) {
				impl.fatal(message);
			}
		}

		@Override
		public void fatal(Object message, Throwable t) {
			if(isFatalEnabled) {
				impl.fatal(message, t);
			}
		}
	}
}
