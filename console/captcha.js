(function(){
    'use strict';
    var config=null;
    function log(){var a=['[Captcha]'];for(var i=0;i<arguments.length;i++)a.push(arguments[i]);console.log.apply(console,a);}
    function pwInputs(){
        var inputs=document.querySelectorAll('input[type=\"password\"]');
        var result=[];
        for(var i=0;i<inputs.length;i++)if(inputs[i].closest('form'))result.push(inputs[i]);
        return result;
    }
    function anchorInput(){
        var pws=pwInputs();
        if(pws.length===0)return null;
        if(pws.length>=2)return pws[pws.length-1];
        return pws[0];
    }
    function ensureWidget(){
        var input=anchorInput();if(!input)return null;
        var w=document.getElementById('captcha-widget');if(w)return w;
        w=document.createElement('div');w.id='captcha-widget';
        w.style.cssText='margin:12px 0 4px 0;padding:8px 0;min-height:50px;max-width:400px;';
        var c=input.closest('.form-input-stack')||input.closest('.form-input')||input.parentElement;
        if(c&&c.parentElement)c.parentElement.insertBefore(w,c.nextSibling);
        else(input.parentElement||document.body).appendChild(w);
        return w;
    }
    function tryRender(){
        if(!config||!config.enabled)return;
        if(!anchorInput())return;
        var w=document.getElementById('captcha-widget');
        if(w&&w.children.length>0)return;
        if(w&&w.dataset.loading==='1')return;
        if(!w){w=ensureWidget();if(!w)return;}
        w.dataset.loading='1';
        log('rendering '+config.provider);
        if(config.provider==='geetest'&&config.geetestId){
            loadScript('https://static.geetest.com/v4/gt4.js',function(){
                w=document.getElementById('captcha-widget');if(!w)return;
                if(window.initGeetest4){
                    window.initGeetest4({captchaId:config.geetestId,product:'float'},function(c){
                        w=document.getElementById('captcha-widget');if(!w)return;
                        c.appendTo('#captcha-widget');
                        c.onReady(function(){
                            w=document.getElementById('captcha-widget');if(w)delete w.dataset.loading;
                            log('geetest READY');
                        });
                        c.onError(function(e){log('geetest ERROR',JSON.stringify(e));});
                        c.onSuccess(function(){
                            var v=c.getValidate();
                            setHidden(['lot_number','captcha_output','pass_token','gen_time'],v);
                            log('geetest SUCCESS');
                        });
                    });
                }else{log('initGeetest4 missing');}
            });
        }else if(config.provider==='cloudflare'&&config.cloudflareSiteKey){
            loadScript('https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit',function(){
                w=document.getElementById('captcha-widget');if(!w||!window.turnstile)return;
                window.turnstile.render('#captcha-widget',{sitekey:config.cloudflareSiteKey});
                delete w.dataset.loading;
            });
        }else if(config.provider==='local'){
            renderLocalCaptcha(w);
        }
    }
    function renderLocalCaptcha(w){
        w.innerHTML='';
        w.style.cssText='margin:12px 0 4px 0;padding:8px 0;min-height:50px;max-width:400px;display:flex;align-items:center;gap:10px;flex-wrap:wrap;';
        var imgUrl=config.captchaImageUrl||'/captcha_image';
        var img=document.createElement('img');
        img.src=imgUrl+'?t='+Date.now();
        img.alt='验证码';
        img.style.cssText='height:44px;border:1px solid #d9d9d9;border-radius:4px;cursor:pointer;';
        img.title='点击刷新验证码';
        var input=document.createElement('input');
        input.type='text';input.name='captcha_code';
        input.placeholder='请输入验证码';input.maxLength=6;
        input.style.cssText='height:44px;width:120px;padding:0 10px;border:1px solid #d9d9d9;border-radius:4px;font-size:15px;letter-spacing:3px;text-align:center;outline:none;';
        input.onfocus=function(){this.style.borderColor='#4e8bf5';};
        input.onblur=function(){this.style.borderColor='#d9d9d9';};
        var refreshBtn=document.createElement('button');refreshBtn.type='button';
        refreshBtn.innerHTML='<svg viewBox=\"0 0 16 16\" width=\"16\" height=\"16\" fill=\"currentColor\"><path d=\"M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z\"/><path d=\"M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z\"/></svg>';
        refreshBtn.title='刷新验证码';
        refreshBtn.style.cssText='height:44px;width:44px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center;';
        var refreshCaptcha=function(){img.src=imgUrl+'?t='+Date.now();input.value='';input.focus();};
        img.onclick=refreshCaptcha;refreshBtn.onclick=refreshCaptcha;
        w.appendChild(img);w.appendChild(input);w.appendChild(refreshBtn);
        delete w.dataset.loading;
        log('local captcha rendered');
    }
    function loadScript(url,cb){
        var s=document.createElement('script');s.src=url;
        s.onload=cb||function(){};s.onerror=function(){log('FAILED',url);};
        document.head.appendChild(s);
    }
    function setHidden(keys,vals){
        var form=document.querySelector('form')||document.body;
        keys.forEach(function(k){
            var el=form.querySelector('input[name=\"'+k+'\"]');
            if(!el){el=document.createElement('input');el.type='hidden';el.name=k;form.appendChild(el);}
            el.value=vals[k]||'';
        });
    }
    function isCaptchaVerified(){
        if(!config||!config.enabled)return true;
        var form=document.querySelector('form');
        if(!form)return true;
        if(config.provider==='geetest'){
            var lot=form.querySelector('input[name=\"lot_number\"]');
            var out=form.querySelector('input[name=\"captcha_output\"]');
            var pas=form.querySelector('input[name=\"pass_token\"]');
            var gen=form.querySelector('input[name=\"gen_time\"]');
            return !!(lot&&lot.value&&out&&out.value&&pas&&pas.value&&gen&&gen.value);
        }
        if(config.provider==='cloudflare'){
            var el=form.querySelector('input[name=\"cf-turnstile-response\"]');
            if(el&&el.value)return true;
            var w=document.getElementById('captcha-widget');
            if(w&&window.turnstile&&window.turnstile.getResponse)return !!window.turnstile.getResponse(w);
            return false;
        }
        if(config.provider==='local'){
            var el=form.querySelector('input[name=\"captcha_code\"]');
            return !!(el&&el.value.trim());
        }
        return true;
    }
    function showToast(msg){
        var existing=document.getElementById('captcha-toast');
        if(existing)existing.remove();
        var toast=document.createElement('div');
        toast.id='captcha-toast';toast.textContent=msg;
        toast.style.cssText='position:fixed;top:20px;left:50%;transform:translateX(-50%);z-index:99999;'
            +'background:#f56c6c;color:#fff;padding:10px 24px;border-radius:6px;font-size:14px;'
            +'box-shadow:0 4px 12px rgba(0,0,0,.15);animation:captchaToastIn .3s ease;pointer-events:none;';
        var style=document.createElement('style');
        style.textContent='@keyframes captchaToastIn{from{opacity:0;transform:translateX(-50%) translateY(-10px)}to{opacity:1;transform:translateX(-50%) translateY(0)}}';
        document.head.appendChild(style);
        document.body.appendChild(toast);
        setTimeout(function(){toast.style.opacity='0';toast.style.transition='opacity .3s ease';
            setTimeout(function(){if(toast.parentNode)toast.remove();},300);},2500);
    }
    function bindFormGuard(){
        var form=document.querySelector('form');
        if(!form)return;
        if(form.dataset.captchaGuarded==='1')return;
        form.dataset.captchaGuarded='1';
        form.addEventListener('submit',function(e){
            if(!isCaptchaVerified()){
                e.preventDefault();
                e.stopPropagation();
                showToast('请先通过人机验证');
            }
        },true);
        log('form guard bound');
    }
    if(window.__captchaConfig){config=window.__captchaConfig;log('cfg',JSON.stringify(config));}
    else{log('FATAL:no cfg');return;}
    setInterval(function(){
        var w=document.getElementById('captcha-widget');
        if(!w||w.children.length===0)tryRender();
    },600);
    new MutationObserver(function(){tryRender();}).observe(document.documentElement,{childList:true,subtree:true});
    if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',function(){tryRender();bindFormGuard();});
    else{tryRender();bindFormGuard();}
})();
