precision mediump float; 
uniform sampler2D u_Texture;
varying vec2 v_TexCoordinate; 

void main() 
{
       vec2 st = v_TexCoordinate.st;
    	st.s = 1. - st.s;
    	//st.t = 1. - st.t;
     gl_FragColor = (texture2D(u_Texture, st));
}