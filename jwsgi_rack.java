import java.util.*;
import java.io.*;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.OutputStrings;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyArray;
import org.jruby.RubyObject;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.Arity;

	
public class jwsgi_rack {

	Ruby rb;
	RubyObject app;

	static class HeadersBlock implements BlockCallback {
		HashMap<String,Object> headers;

		public HeadersBlock(HashMap<String,Object> h) {
			headers = h;
		}
                public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
			RubyArray arg = (RubyArray) args[0];
			String h_key = (String) arg.get(0);
			String h_value = (String) arg.get(1);
			String[] values = h_value.split("\n"); 
			for(int i=0;i<values.length;i++) {	
				headers.put(h_key, values[i]);
			}
                        return null;
                }
        }

	static class BodyBlock implements BlockCallback {
		List<String> body_list;

                public BodyBlock(List<String> bl) {
                        body_list = bl;
                }
                public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                        RubyString arg = (RubyString) args[0];
			body_list.add(arg.asJavaString());
                        return null;
                }
        }
	
	public jwsgi_rack() throws Exception {
		if (!uwsgi.opt.containsKey("jwsgi-rack")) {
			throw new Exception("you have to specify the path of a rack application with the \"jwsgi-rack\" virtual option");	
		}
		System.out.println(uwsgi.opt.toString());
		System.out.println("launching jruby...");
		RubyInstanceConfig config = new RubyInstanceConfig();
		rb = Ruby.newInstance();

		config.getOutput().println(OutputStrings.getVersionString(config.getCompatVersion()));

		rb.getLoadService().require("rubygems");
		rb.getObject().callMethod("require", rb.newString("rack"));

		RubyModule rack_module = rb.getModule("Rack");
		if (rack_module == null) {
			throw new Exception("unable to find Rack::Builder");
		}
		RubyClass rack_builder = rack_module.getClass("Builder");
		if (rack_builder == null) {
			throw new Exception("unable to find Rack::Builder");
		}
		RubyArray rackup = (RubyArray) rack_builder.callMethod("parse_file", rb.newString((String)uwsgi.opt.get("jwsgi-rack")));
		app = (RubyObject) rackup.get(0);
	}

	public Object[] application(HashMap env) throws Exception {

		IRubyObject []no_args = { };
		RubyHash rb_env = new RubyHash(rb);
		Iterator it = env.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry entry = (Map.Entry)it.next();
			Object o_key = entry.getKey();
			Object o_value = entry.getValue();
			if (o_key instanceof String && o_value instanceof String) {
				String str_key = (String) o_key;
				String str_value = (String) o_value;
				if (str_value.length() > 0 ||
					str_key == "REQUEST_METHOD" ||
					str_key == "SCRIPT_NAME" ||
					str_key == "PATH_INFO" ||
					str_key == "QUERY_STRING" ||
					str_key == "SERVER_NAME" ||
					str_key == "SERVER_PORT") {
					RubyString key = rb.newString( str_key );
					RubyString value = rb.newString( str_value );
					rb_env.put(key, value);
				}
			}
		}

		rb_env.put( rb.newString("rack.input"), rb.newString(""));

		RubyArray rb_response = (RubyArray) app.callMethod(rb.getCurrentContext(), "call", rb_env);
		long status = (Long) rb_response.get(0);

		RubyObject rb_headers = (RubyObject) rb_response.get(1);
		if (!rb_headers.respondsTo("each")) {
			throw new Exception("Rack headers object must respond to \"each\"");
		}
		HashMap<String,Object> headers = new HashMap<String,Object>();
		HeadersBlock hb = new HeadersBlock(headers);
		Block headers_iterator = CallBlock.newCallClosure(rb_headers, null, Arity.ONE_ARGUMENT, hb, rb.getCurrentContext());
		rb_headers.callMethod(rb.getCurrentContext(), "each", no_args, headers_iterator);

		Object generic_body = null;

		RubyObject body = (RubyObject) rb_response.get(2);
		// use sendfile()
		if (body.respondsTo("to_path")) {
			RubyString rb_path = (RubyString) body.callMethod(rb.getCurrentContext(), "to_path");
			generic_body = new File(rb_path.asJavaString());	
		}
		else if (body.respondsTo("each")) {
		List<String> body_list = new ArrayList<String>();
		BodyBlock bb = new BodyBlock(body_list);
		Block body_iterator = CallBlock.newCallClosure(body, null, Arity.ONE_ARGUMENT, bb, rb.getCurrentContext());	
		body.callMethod(rb.getCurrentContext(), "each", no_args, body_iterator);
		generic_body = body_list;
	}

	// call it always
	if (body.respondsTo("to_close")) {
	}

        Object[] response = { status, headers, generic_body };
        return response;
    }
}
