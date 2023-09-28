export interface Cfg<T >{
classname:string,
message:T,
}
const Panel = <T,> ( cfg:Cfg<T>):JSX.Element  =>{
                  return (<div className={cfg.classname}>{String(cfg.message)}</div>
                  )  ;
    }